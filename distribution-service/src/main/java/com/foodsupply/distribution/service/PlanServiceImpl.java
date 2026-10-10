package com.foodsupply.distribution.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.foodsupply.distribution.client.InventoryClient;
import com.foodsupply.distribution.client.ProcurementClient;
import com.foodsupply.distribution.client.ProgrammeClient;
import com.foodsupply.distribution.dto.DeliveredItem;
import com.foodsupply.distribution.dto.DeliveryRequest;
import com.foodsupply.distribution.dto.DistributionSummary;
import com.foodsupply.distribution.dto.PageResponse;
import com.foodsupply.distribution.dto.PlanListItem;
import com.foodsupply.distribution.dto.PlanRequest;
import com.foodsupply.distribution.dto.PlanResponse;
import com.foodsupply.distribution.dto.PreviewResponse;
import com.foodsupply.distribution.dto.PreviewResponse.PreviewItem;
import com.foodsupply.distribution.dto.RaiseProcurementRequest;
import com.foodsupply.distribution.entity.CentreStatus;
import com.foodsupply.distribution.entity.DistributionAllocation;
import com.foodsupply.distribution.entity.DistributionCentre;
import com.foodsupply.distribution.entity.DistributionItem;
import com.foodsupply.distribution.entity.DistributionPlan;
import com.foodsupply.distribution.entity.DistributionRecord;
import com.foodsupply.distribution.entity.FoodUnit;
import com.foodsupply.distribution.entity.PlanBeneficiary;
import com.foodsupply.distribution.entity.PlanStatus;
import com.foodsupply.distribution.entity.RecordStatus;
import com.foodsupply.distribution.exception.BusinessException;
import com.foodsupply.distribution.exception.InvalidRequestException;
import com.foodsupply.distribution.exception.ResourceNotFoundException;
import com.foodsupply.distribution.mapper.PlanMapper;
import com.foodsupply.distribution.repository.CentreRepository;
import com.foodsupply.distribution.repository.PlanRepository;
import com.foodsupply.distribution.repository.RecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanServiceImpl implements PlanService {

    private static final String REF = "DISTRIBUTION";
    private static final Set<String> SORT_FIELDS = Set.of("id", "planNumber", "startDate", "endDate", "status", "createdOn");

    private final PlanRepository planRepository;
    private final CentreRepository centreRepository;
    private final RecordRepository recordRepository;
    private final ProgrammeClient programmeClient;
    private final InventoryClient inventoryClient;
    private final ProcurementClient procurementClient;

    @Value("${app.procurement.default-unit-price:50}")
    private BigDecimal defaultUnitPrice;

    @Override
    public PageResponse<PlanListItem> list(String q, PlanStatus status, Long programmeId, Long centreId,
                                           int page, int size, String sort, String dir) {
        Specification<DistributionPlan> spec = (root, query, cb) -> cb.conjunction();
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("planNumber")), like),
                    cb.like(cb.lower(root.get("programmeName")), like)));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (programmeId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("programmeId"), programmeId));
        }
        if (centreId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("centre").get("id"), centreId));
        }
        String field = SORT_FIELDS.contains(sort) ? sort : "id";
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 1000), Sort.by(direction, field));
        return PageResponse.of(planRepository.findAll(spec, pageable).map(PlanMapper::toListItem));
    }

    @Override
    public PlanResponse get(Long id) {
        return PlanMapper.toPlan(find(id));
    }

    @Override
    public PreviewResponse preview(PlanRequest r) {
        validateDates(r.getStartDate(), r.getEndDate());
        Long centreId = r.getCentreId();
        String centreName = null;
        if (centreId != null) {
            centreName = findCentre(centreId).getName();
        }
        JsonNode req = programmeClient.requirements(r.getProgrammeId(), r.getStartDate(), r.getEndDate());
        List<PreviewItem> items = previewItems(req);
        List<PreviewItem> shortages = items.stream().filter(i -> !i.sufficient()).toList();
        List<PreviewItem> expiring = items.stream().filter(i -> i.expiringSoonQty().signum() > 0).toList();
        return new PreviewResponse(r.getProgrammeId(), req.path("programmeName").asText(), centreId, centreName,
                r.getStartDate(), r.getEndDate(), req.path("enrolledPeople").asInt(), items, shortages, expiring,
                shortages.isEmpty());
    }

    /**
     * Creates the plan and reserves stock in inventory-service. If inventory answers 409 (shortage) the exception
     * propagates, the transaction rolls back and nothing is saved.
     */
    @Override
    @Transactional
    public PlanResponse create(PlanRequest r) {
        validateDates(r.getStartDate(), r.getEndDate());
        if (r.getCentreId() == null) {
            throw new InvalidRequestException("centreId is required to create a plan");
        }
        DistributionCentre centre = findCentre(r.getCentreId());
        if (centre.getStatus() != CentreStatus.ACTIVE) {
            throw new BusinessException("Distribution centre '" + centre.getName() + "' is INACTIVE");
        }
        JsonNode req = programmeClient.requirements(r.getProgrammeId(), r.getStartDate(), r.getEndDate());
        JsonNode reqItems = req.path("items");
        if (!reqItems.isArray() || reqItems.size() == 0) {
            throw new InvalidRequestException("The programme has no meal-plan items in the selected period");
        }
        JsonNode participants = programmeClient.participants(r.getProgrammeId());

        DistributionPlan plan = DistributionPlan.builder().programmeId(r.getProgrammeId())
                .programmeName(req.path("programmeName").asText()).centre(centre)
                .startDate(r.getStartDate()).endDate(r.getEndDate()).status(PlanStatus.PLANNED)
                .remarks(r.getRemarks()).createdOn(LocalDate.now()).enrolledPeople(req.path("enrolledPeople").asInt())
                .build();
        for (JsonNode n : reqItems) {
            plan.getItems().add(DistributionItem.builder().plan(plan).foodItemId(n.path("foodItemId").asLong())
                    .foodItemName(n.path("foodItemName").asText()).unit(FoodUnit.valueOf(n.path("unit").asText()))
                    .plannedQty(n.path("totalQuantity").decimalValue()).build());
        }
        for (JsonNode p : participants) {
            if ("ENROLLED".equals(p.path("status").asText())) {
                plan.getBeneficiaries().add(PlanBeneficiary.builder().plan(plan).beneficiaryId(p.path("beneficiaryId").asLong())
                        .beneficiaryName(p.path("beneficiaryName").asText()).peopleCovered(p.path("peopleCovered").asInt(1)).build());
            }
        }
        if (plan.getBeneficiaries().isEmpty()) {
            throw new BusinessException("The programme has no enrolled participants");
        }

        DistributionPlan saved = planRepository.saveAndFlush(plan);
        saved.setPlanNumber(String.format("DP-%d-%04d", LocalDate.now().getYear(), saved.getId()));

        JsonNode res = inventoryClient.reserve(reserveBody(saved));   // ShortageException => rollback, nothing created
        try {
            Map<Long, DistributionItem> byFood = new HashMap<>();
            for (DistributionItem it : saved.getItems()) {
                byFood.put(it.getFoodItemId(), it);
            }
            for (JsonNode a : res.path("allocations")) {
                DistributionItem it = byFood.get(a.path("foodItemId").asLong());
                if (it == null) {
                    continue;
                }
                LocalDate expiry = a.hasNonNull("expiryDate") ? LocalDate.parse(a.get("expiryDate").asText()) : null;
                it.getAllocations().add(DistributionAllocation.builder().item(it).batchId(a.path("batchId").asLong())
                        .batchNumber(a.path("batchNumber").asText(null)).expiryDate(expiry)
                        .quantity(a.path("quantity").decimalValue()).build());
            }
            saved.setStatus(PlanStatus.RESERVED);
            planRepository.saveAndFlush(saved);
        } catch (RuntimeException e) {
            log.error("Saving reservation details failed; releasing stock for plan {}", saved.getId());
            releaseQuietly(saved.getId());
            throw e;
        }
        log.info("Plan {} created and stock reserved", saved.getPlanNumber());
        return PlanMapper.toPlan(saved);
    }

    @Override
    @Transactional
    public PlanResponse dispatch(Long id) {
        DistributionPlan plan = find(id);
        if (plan.getStatus() != PlanStatus.RESERVED) {
            throw new BusinessException("Only RESERVED plans can be dispatched (current status: " + plan.getStatus() + ")");
        }
        inventoryClient.dispatch(refBody(id));
        for (DistributionItem it : plan.getItems()) {
            it.setDispatchedQty(it.getPlannedQty());
        }
        plan.setStatus(PlanStatus.DISPATCHED);
        plan.setDispatchedOn(LocalDate.now());
        planRepository.save(plan);
        log.info("Plan {} dispatched", plan.getPlanNumber());
        return PlanMapper.toPlan(plan);
    }

    @Override
    @Transactional
    public PlanResponse deliver(Long id, DeliveryRequest r) {
        DistributionPlan plan = find(id);
        if (plan.getStatus() != PlanStatus.DISPATCHED) {
            throw new BusinessException("Only DISPATCHED plans can be delivered (current status: " + plan.getStatus() + ")");
        }
        Map<Long, BigDecimal> delivered = new HashMap<>();
        if (r == null || r.getItems() == null || r.getItems().isEmpty()) {
            for (DistributionItem it : plan.getItems()) {
                delivered.put(it.getFoodItemId(), it.getDispatchedQty());
            }
        } else {
            for (DeliveredItem di : r.getItems()) {
                DistributionItem match = null;
                for (DistributionItem it : plan.getItems()) {
                    if (it.getFoodItemId().equals(di.getFoodItemId())) {
                        match = it;
                    }
                }
                if (match == null) {
                    throw new InvalidRequestException("Food item " + di.getFoodItemId() + " is not part of this plan");
                }
                if (di.getDeliveredQuantity().compareTo(match.getDispatchedQty()) > 0) {
                    throw new BusinessException("Delivered quantity of " + match.getFoodItemName()
                            + " exceeds the dispatched quantity " + match.getDispatchedQty());
                }
                delivered.put(di.getFoodItemId(), di.getDeliveredQuantity());
            }
        }

        LocalDate on = (r != null && r.getDeliveredOn() != null) ? r.getDeliveredOn() : LocalDate.now();
        boolean allFull = true;
        boolean anyDelivered = false;
        for (DistributionItem it : plan.getItems()) {
            BigDecimal q = delivered.getOrDefault(it.getFoodItemId(), BigDecimal.ZERO).setScale(3, RoundingMode.HALF_UP);
            it.setDeliveredQty(q);
            if (q.compareTo(it.getDispatchedQty()) < 0) {
                allFull = false;
            }
            if (q.signum() > 0) {
                anyDelivered = true;
            }
        }
        PlanStatus result = !anyDelivered ? PlanStatus.FAILED : (allFull ? PlanStatus.DELIVERED : PlanStatus.PARTIAL);

        int totalPeople = 0;
        for (PlanBeneficiary b : plan.getBeneficiaries()) {
            totalPeople += b.getPeopleCovered();
        }
        List<DistributionRecord> records = new ArrayList<>();
        for (PlanBeneficiary b : plan.getBeneficiaries()) {
            for (DistributionItem it : plan.getItems()) {
                BigDecimal share = totalPeople == 0 ? BigDecimal.ZERO
                        : it.getDeliveredQty().multiply(BigDecimal.valueOf(b.getPeopleCovered()))
                        .divide(BigDecimal.valueOf(totalPeople), 3, RoundingMode.HALF_UP);
                records.add(DistributionRecord.builder().plan(plan).beneficiaryId(b.getBeneficiaryId())
                        .beneficiaryName(b.getBeneficiaryName()).foodItemId(it.getFoodItemId())
                        .foodItemName(it.getFoodItemName()).unit(it.getUnit()).quantity(share).deliveredOn(on)
                        .status(recordStatus(it)).build());
            }
        }
        recordRepository.saveAll(records);
        plan.setStatus(result);
        plan.setDeliveredOn(on);
        if (r != null && r.getRemarks() != null && !r.getRemarks().isBlank()) {
            plan.setRemarks(r.getRemarks());
        }
        planRepository.save(plan);
        log.info("Plan {} delivery confirmed: {}", plan.getPlanNumber(), result);
        return PlanMapper.toPlan(plan);
    }

    @Override
    @Transactional
    public PlanResponse cancel(Long id) {
        DistributionPlan plan = find(id);
        if (plan.getStatus() != PlanStatus.PLANNED && plan.getStatus() != PlanStatus.RESERVED) {
            throw new BusinessException("Only PLANNED or RESERVED plans can be cancelled (current status: " + plan.getStatus() + ")");
        }
        if (plan.getStatus() == PlanStatus.RESERVED) {
            inventoryClient.release(refBody(id));
        }
        plan.setStatus(PlanStatus.CANCELLED);
        planRepository.save(plan);
        log.info("Plan {} cancelled", plan.getPlanNumber());
        return PlanMapper.toPlan(plan);
    }

    @Override
    public JsonNode raiseProcurement(RaiseProcurementRequest r) {
        validateDates(r.getStartDate(), r.getEndDate());
        JsonNode req = programmeClient.requirements(r.getProgrammeId(), r.getStartDate(), r.getEndDate());
        List<Map<String, Object>> lines = new ArrayList<>();
        for (JsonNode n : req.path("shortages")) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("foodItemId", n.path("foodItemId").asLong());
            m.put("foodItemName", n.path("foodItemName").asText());
            m.put("unit", n.path("unit").asText());
            m.put("quantity", n.path("shortage").decimalValue());
            m.put("estimatedUnitPrice", defaultUnitPrice);
            lines.add(m);
        }
        if (lines.isEmpty()) {
            throw new BusinessException("No shortages for this programme and period - nothing to procure");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("requestedBy", r.getRequestedBy() != null && !r.getRequestedBy().isBlank() ? r.getRequestedBy() : "distribution-service");
        body.put("reason", "Shortage for " + req.path("programmeName").asText() + " (" + r.getStartDate() + " to " + r.getEndDate() + ")");
        body.put("source", "SHORTAGE");
        body.put("submit", true);
        if (!r.getStartDate().isBefore(LocalDate.now())) {
            body.put("needByDate", r.getStartDate().toString());
        }
        body.put("items", lines);
        JsonNode created = procurementClient.createRequest(body);
        log.info("Raised shortage procurement request for programme {} ({} items)", r.getProgrammeId(), lines.size());
        return created;
    }

    @Override
    public DistributionSummary summary() {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        long total = 0;
        for (PlanStatus s : PlanStatus.values()) {
            long c = planRepository.countByStatus(s);
            byStatus.put(s.name(), c);
            total += c;
        }
        long pending = byStatus.get("PLANNED") + byStatus.get("RESERVED") + byStatus.get("DISPATCHED");
        return new DistributionSummary(total, byStatus, pending, byStatus.get("DELIVERED"),
                centreRepository.count(), centreRepository.countByStatus(CentreStatus.ACTIVE), recordRepository.count());
    }

    private List<PreviewItem> previewItems(JsonNode req) {
        List<PreviewItem> list = new ArrayList<>();
        for (JsonNode n : req.path("items")) {
            list.add(new PreviewItem(n.path("foodItemId").asLong(), n.path("foodItemName").asText(),
                    FoodUnit.valueOf(n.path("unit").asText()), n.path("totalQuantity").decimalValue(),
                    n.path("usableQuantity").decimalValue(), n.path("shortage").decimalValue(),
                    n.path("expiringSoonQuantity").decimalValue(), n.path("sufficient").asBoolean(true)));
        }
        return list;
    }

    private Map<String, Object> reserveBody(DistributionPlan plan) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (DistributionItem it : plan.getItems()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("foodItemId", it.getFoodItemId());
            m.put("requiredQuantity", it.getPlannedQty());
            items.add(m);
        }
        Map<String, Object> body = refBody(plan.getId());
        body.put("items", items);
        return body;
    }

    private Map<String, Object> refBody(Long planId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("referenceType", REF);
        body.put("referenceId", planId);
        return body;
    }

    private void releaseQuietly(Long planId) {
        try {
            inventoryClient.release(refBody(planId));
        } catch (RuntimeException e) {
            log.error("Could not release reservation for plan {}: {}", planId, e.getMessage());
        }
    }

    private RecordStatus recordStatus(DistributionItem it) {
        if (it.getDeliveredQty().signum() == 0) {
            return RecordStatus.FAILED;
        }
        return it.getDeliveredQty().compareTo(it.getDispatchedQty()) < 0 ? RecordStatus.PARTIAL : RecordStatus.DELIVERED;
    }

    private DistributionPlan find(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Distribution plan not found: " + id));
    }

    private DistributionCentre findCentre(Long id) {
        return centreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Distribution centre not found: " + id));
    }

    private void validateDates(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new InvalidRequestException("endDate must not be before startDate");
        }
    }
}

package com.foodsupply.procurement.service;

import com.foodsupply.procurement.client.FoodNutritionClient;
import com.foodsupply.procurement.dto.FoodItemInfo;
import com.foodsupply.procurement.dto.PageResponse;
import com.foodsupply.procurement.dto.ProcurementRequestInput;
import com.foodsupply.procurement.dto.RequestItemInput;
import com.foodsupply.procurement.dto.RequestResponse;
import com.foodsupply.procurement.entity.FoodUnit;
import com.foodsupply.procurement.entity.ProcurementRequest;
import com.foodsupply.procurement.entity.RequestItem;
import com.foodsupply.procurement.entity.RequestSource;
import com.foodsupply.procurement.entity.RequestStatus;
import com.foodsupply.procurement.exception.BusinessException;
import com.foodsupply.procurement.exception.DownstreamServiceException;
import com.foodsupply.procurement.exception.InvalidRequestException;
import com.foodsupply.procurement.exception.ResourceNotFoundException;
import com.foodsupply.procurement.mapper.ProcurementMapper;
import com.foodsupply.procurement.repository.ProcurementRequestRepository;
import com.foodsupply.procurement.util.PageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProcurementRequestServiceImpl implements ProcurementRequestService {

    private static final Set<String> SORT_FIELDS =
            Set.of("id", "requestNumber", "createdOn", "needByDate", "status", "estimatedCost", "source");

    private final ProcurementRequestRepository requestRepository;
    private final FoodNutritionClient foodNutritionClient;

    @Override
    public PageResponse<RequestResponse> list(String q, RequestStatus status, RequestSource source, int page,
                                              int size, String sort, String dir) {
        Specification<ProcurementRequest> spec = (root, query, cb) -> cb.conjunction();
        if (q != null && !q.isBlank()) {
            String pattern = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.<String>get("requestNumber")), pattern),
                    cb.like(cb.lower(root.<String>get("reason")), pattern),
                    cb.like(cb.lower(root.<String>get("requestedBy")), pattern)));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (source != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("source"), source));
        }
        Pageable pageable = PageUtil.of(page, size, sort, dir, SORT_FIELDS, "id", Sort.Direction.DESC);
        Page<RequestResponse> result =
                requestRepository.findAll(spec, pageable).map(ProcurementMapper::toRequestResponse);
        return PageResponse.of(result);
    }

    @Override
    public RequestResponse get(Long id) {
        return ProcurementMapper.toRequestResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public RequestResponse create(ProcurementRequestInput input) {
        LocalDate today = LocalDate.now();
        validateNeedBy(input.getNeedByDate(), today);

        ProcurementRequest request = ProcurementRequest.builder()
                .requestNumber(nextRequestNumber(today.getYear()))
                .source(input.getSource() != null ? input.getSource() : RequestSource.MANUAL)
                .status(Boolean.TRUE.equals(input.getSubmit()) ? RequestStatus.SUBMITTED : RequestStatus.DRAFT)
                .reason(input.getReason())
                .requestedBy(input.getRequestedBy().trim())
                .createdOn(today)
                .needByDate(input.getNeedByDate())
                .build();
        applyItems(request, input.getItems());
        request.setEstimatedCost(computeCost(request));

        ProcurementRequest saved = requestRepository.save(request);
        log.info("Created procurement request {} ({}, {}) with {} item(s)", saved.getRequestNumber(),
                saved.getSource(), saved.getStatus(), saved.getItems().size());
        return ProcurementMapper.toRequestResponse(saved);
    }

    @Override
    @Transactional
    public RequestResponse update(Long id, ProcurementRequestInput input) {
        ProcurementRequest request = findOrThrow(id);
        if (request.getStatus() != RequestStatus.DRAFT) {
            throw new BusinessException("Request " + request.getRequestNumber() + " is " + request.getStatus()
                    + "; only DRAFT requests can be edited");
        }
        validateNeedBy(input.getNeedByDate(), request.getCreatedOn());
        request.setReason(input.getReason());
        request.setRequestedBy(input.getRequestedBy().trim());
        request.setNeedByDate(input.getNeedByDate());
        if (input.getSource() != null) {
            request.setSource(input.getSource());
        }
        applyItems(request, input.getItems());
        request.setEstimatedCost(computeCost(request));
        log.info("Updated procurement request {}", request.getRequestNumber());
        return ProcurementMapper.toRequestResponse(request);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ProcurementRequest request = findOrThrow(id);
        if (request.getStatus() != RequestStatus.DRAFT && request.getStatus() != RequestStatus.REJECTED) {
            throw new BusinessException("Request " + request.getRequestNumber() + " is " + request.getStatus()
                    + "; only DRAFT or REJECTED requests can be deleted");
        }
        requestRepository.delete(request);
        log.info("Deleted procurement request {}", request.getRequestNumber());
    }

    @Override
    @Transactional
    public RequestResponse submit(Long id) {
        return changeStatus(id, RequestStatus.DRAFT, RequestStatus.SUBMITTED, null);
    }

    @Override
    @Transactional
    public RequestResponse approve(Long id) {
        return changeStatus(id, RequestStatus.SUBMITTED, RequestStatus.APPROVED, null);
    }

    @Override
    @Transactional
    public RequestResponse reject(Long id, String reason) {
        return changeStatus(id, RequestStatus.SUBMITTED, RequestStatus.REJECTED, reason);
    }

    // ------------------------------------------------------------------ helpers

    private RequestResponse changeStatus(Long id, RequestStatus expected, RequestStatus target, String remarks) {
        ProcurementRequest request = findOrThrow(id);
        if (request.getStatus() != expected) {
            throw new BusinessException(String.format("Request %s is %s; only %s requests can be moved to %s",
                    request.getRequestNumber(), request.getStatus(), expected, target));
        }
        request.setStatus(target);
        if (remarks != null && !remarks.isBlank()) {
            request.setDecisionRemarks(remarks.trim());
        }
        log.info("Request {} {} -> {}", request.getRequestNumber(), expected, target);
        return ProcurementMapper.toRequestResponse(request);
    }

    private ProcurementRequest findOrThrow(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Procurement request not found with id " + id));
    }

    private void validateNeedBy(LocalDate needBy, LocalDate reference) {
        if (needBy != null && needBy.isBefore(reference)) {
            throw new InvalidRequestException("needByDate cannot be before " + reference);
        }
    }

    private String nextRequestNumber(int year) {
        String prefix = "PR-" + year + "-";
        Optional<ProcurementRequest> last =
                requestRepository.findTopByRequestNumberStartingWithOrderByRequestNumberDesc(prefix);
        int next = 1;
        if (last.isPresent()) {
            String number = last.get().getRequestNumber();
            try {
                next = Integer.parseInt(number.substring(number.lastIndexOf('-') + 1)) + 1;
            } catch (NumberFormatException e) {
                log.warn("Unexpected request number format: {}", number);
            }
        }
        return String.format("%s%04d", prefix, next);
    }

    /** Replaces the items of the request; resolves missing names/units from food-nutrition-service. */
    private void applyItems(ProcurementRequest request, List<RequestItemInput> inputs) {
        Set<Long> seen = new HashSet<>();
        List<Long> toResolve = new ArrayList<>();
        for (RequestItemInput in : inputs) {
            if (!seen.add(in.getFoodItemId())) {
                throw new InvalidRequestException("Food item " + in.getFoodItemId() + " appears more than once");
            }
            boolean hasName = in.getFoodItemName() != null && !in.getFoodItemName().isBlank();
            if (!hasName || in.getUnit() == null) {
                toResolve.add(in.getFoodItemId());
            }
        }

        Map<Long, FoodItemInfo> resolved = new HashMap<>();
        if (!toResolve.isEmpty()) {
            for (FoodItemInfo info : foodNutritionClient.getByIds(toResolve)) {
                resolved.put(info.id(), info);
            }
            for (Long foodItemId : toResolve) {
                if (!resolved.containsKey(foodItemId)) {
                    throw new InvalidRequestException(
                            "Food item " + foodItemId + " does not exist in food-nutrition-service");
                }
            }
        }

        request.clearItems();
        for (RequestItemInput in : inputs) {
            String name;
            FoodUnit unit;
            FoodItemInfo info = resolved.get(in.getFoodItemId());
            if (info != null) {
                if (Boolean.FALSE.equals(info.active())) {
                    throw new BusinessException("Food item '" + info.name() + "' is inactive and cannot be procured");
                }
                name = info.name();
                unit = parseUnit(info.unit());
            } else {
                name = in.getFoodItemName().trim();
                unit = in.getUnit();
            }
            request.addItem(RequestItem.builder()
                    .foodItemId(in.getFoodItemId())
                    .foodItemName(name)
                    .unit(unit)
                    .quantity(in.getQuantity())
                    .estimatedUnitPrice(in.getEstimatedUnitPrice())
                    .build());
        }
    }

    private FoodUnit parseUnit(String unit) {
        try {
            return FoodUnit.valueOf(unit == null ? "" : unit.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DownstreamServiceException("food-nutrition-service returned an unknown unit '" + unit + "'");
        }
    }

    private BigDecimal computeCost(ProcurementRequest request) {
        BigDecimal total = BigDecimal.ZERO;
        for (RequestItem item : request.getItems()) {
            if (item.getEstimatedUnitPrice() != null) {
                total = total.add(item.getQuantity().multiply(item.getEstimatedUnitPrice()));
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }
}

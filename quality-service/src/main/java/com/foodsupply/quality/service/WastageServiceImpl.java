package com.foodsupply.quality.service;

import com.foodsupply.quality.client.InventoryClient;
import com.foodsupply.quality.dto.BatchInfo;
import com.foodsupply.quality.dto.PageResponse;
import com.foodsupply.quality.dto.StockInfo;
import com.foodsupply.quality.dto.WastageRequest;
import com.foodsupply.quality.dto.WastageResponse;
import com.foodsupply.quality.dto.WastageSummaryResponse;
import com.foodsupply.quality.entity.FoodUnit;
import com.foodsupply.quality.entity.WastageReason;
import com.foodsupply.quality.entity.WastageRecord;
import com.foodsupply.quality.exception.DownstreamServiceException;
import com.foodsupply.quality.exception.InvalidRequestException;
import com.foodsupply.quality.exception.ResourceNotFoundException;
import com.foodsupply.quality.mapper.QualityMapper;
import com.foodsupply.quality.repository.WastageRecordRepository;
import com.foodsupply.quality.util.PageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WastageServiceImpl implements WastageService {

    private static final Set<String> SORT_FIELDS =
            Set.of("id", "recordedOn", "foodItemName", "quantity", "reason");

    private final WastageRecordRepository repository;
    private final InventoryClient inventoryClient;

    @Override
    public PageResponse<WastageResponse> list(String q, WastageReason reason, Long foodItemId, Long batchId,
                                              LocalDate from, LocalDate to, int page, int size,
                                              String sort, String dir) {
        Specification<WastageRecord> spec = dateSpec(from, to);

        if (q != null && !q.isBlank()) {
            final String pattern = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("foodItemName")), pattern),
                    cb.like(cb.lower(root.get("batchNumber")), pattern),
                    cb.like(cb.lower(root.get("recordedBy")), pattern)));
        }
        if (reason != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("reason"), reason));
        }
        if (foodItemId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("foodItemId"), foodItemId));
        }
        if (batchId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("batchId"), batchId));
        }

        Pageable pageable = PageUtil.build(page, size, sort, dir, SORT_FIELDS, "recordedOn", Sort.Direction.DESC);
        return PageResponse.of(repository.findAll(spec, pageable).map(QualityMapper::toWastageResponse));
    }

    @Override
    public WastageResponse get(Long id) {
        WastageRecord record = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Wastage record not found with id " + id));
        return QualityMapper.toWastageResponse(record);
    }

    /**
     * Writes the stock off in inventory FIRST; the wastage record is only saved when
     * inventory accepted it (409 if the quantity exceeds the unreserved on-hand stock).
     */
    @Override
    @Transactional
    public WastageResponse create(WastageRequest request) {
        BatchInfo batch = inventoryClient.getBatch(request.getBatchId());
        if (batch == null || batch.foodItemId() == null) {
            throw new DownstreamServiceException("inventory-service returned an unexpected answer for batch "
                    + request.getBatchId());
        }

        FoodUnit unit = request.getUnit() != null ? request.getUnit() : resolveUnit(batch.foodItemId());

        inventoryClient.writeOff(request.getBatchId(), request.getQuantity(), request.getReason().name(), null);

        WastageRecord saved = repository.save(WastageRecord.builder()
                .batchId(request.getBatchId())
                .batchNumber(batch.batchNumber())
                .foodItemId(batch.foodItemId())
                .foodItemName(batch.foodItemName())
                .unit(unit)
                .quantity(request.getQuantity())
                .reason(request.getReason())
                .disposalMethod(request.getDisposalMethod())
                .recordedOn(request.getRecordedOn() != null ? request.getRecordedOn() : LocalDate.now())
                .recordedBy(request.getRecordedBy().trim())
                .remarks(request.getRemarks())
                .build());

        log.info("Wastage {} saved: {} {} of {} (batch {}), reason {}", saved.getId(), saved.getQuantity(),
                saved.getUnit(), saved.getFoodItemName(), saved.getBatchNumber(), saved.getReason());
        return QualityMapper.toWastageResponse(saved);
    }

    @Override
    public WastageSummaryResponse summary(LocalDate from, LocalDate to) {
        List<WastageRecord> records = repository.findAll(dateSpec(from, to));

        BigDecimal total = BigDecimal.ZERO;
        Map<FoodUnit, BigDecimal> byUnit = new EnumMap<>(FoodUnit.class);
        Map<WastageReason, BigDecimal> qtyByReason = new EnumMap<>(WastageReason.class);
        Map<WastageReason, Long> countByReason = new EnumMap<>(WastageReason.class);
        Map<String, BigDecimal> qtyByMonth = new TreeMap<>();
        Map<String, Long> countByMonth = new TreeMap<>();

        for (WastageRecord r : records) {
            total = total.add(r.getQuantity());
            byUnit.merge(r.getUnit(), r.getQuantity(), BigDecimal::add);
            qtyByReason.merge(r.getReason(), r.getQuantity(), BigDecimal::add);
            countByReason.merge(r.getReason(), 1L, Long::sum);
            String month = YearMonth.from(r.getRecordedOn()).toString();
            qtyByMonth.merge(month, r.getQuantity(), BigDecimal::add);
            countByMonth.merge(month, 1L, Long::sum);
        }

        Map<String, BigDecimal> quantityByUnit = new LinkedHashMap<>();
        for (Map.Entry<FoodUnit, BigDecimal> e : byUnit.entrySet()) {
            quantityByUnit.put(e.getKey().name(), e.getValue());
        }

        List<WastageSummaryResponse.ReasonSummary> byReason = new ArrayList<>();
        for (Map.Entry<WastageReason, BigDecimal> e : qtyByReason.entrySet()) {
            byReason.add(new WastageSummaryResponse.ReasonSummary(
                    e.getKey().name(), countByReason.get(e.getKey()), e.getValue()));
        }

        List<WastageSummaryResponse.MonthSummary> byMonth = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : qtyByMonth.entrySet()) {
            byMonth.add(new WastageSummaryResponse.MonthSummary(
                    e.getKey(), countByMonth.get(e.getKey()), e.getValue()));
        }

        return new WastageSummaryResponse(from, to, records.size(), total, quantityByUnit, byReason, byMonth);
    }

    private Specification<WastageRecord> dateSpec(LocalDate from, LocalDate to) {
        Specification<WastageRecord> spec = (root, query, cb) -> cb.conjunction();
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.<LocalDate>get("recordedOn"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.<LocalDate>get("recordedOn"), to));
        }
        return spec;
    }

    private FoodUnit resolveUnit(Long foodItemId) {
        StockInfo stock = inventoryClient.getStock(foodItemId);
        if (stock == null || stock.unit() == null) {
            throw new InvalidRequestException("Could not determine the unit of food item " + foodItemId
                    + "; please supply 'unit' in the request");
        }
        try {
            return FoodUnit.valueOf(stock.unit());
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException("Unknown unit '" + stock.unit() + "' returned by inventory-service");
        }
    }
}

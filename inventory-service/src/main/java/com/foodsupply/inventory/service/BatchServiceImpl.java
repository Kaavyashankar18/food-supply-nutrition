package com.foodsupply.inventory.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.foodsupply.inventory.dto.BatchResponse;
import com.foodsupply.inventory.dto.PageResponse;
import com.foodsupply.inventory.dto.WriteOffRequest;
import com.foodsupply.inventory.entity.MovementType;
import com.foodsupply.inventory.entity.QualityStatus;
import com.foodsupply.inventory.entity.StockBatch;
import com.foodsupply.inventory.entity.StockItem;
import com.foodsupply.inventory.entity.StockMovement;
import com.foodsupply.inventory.exception.BusinessException;
import com.foodsupply.inventory.exception.InvalidRequestException;
import com.foodsupply.inventory.exception.ResourceNotFoundException;
import com.foodsupply.inventory.mapper.BatchMapper;
import com.foodsupply.inventory.repository.StockBatchRepository;
import com.foodsupply.inventory.repository.StockItemRepository;
import com.foodsupply.inventory.repository.StockMovementRepository;
import com.foodsupply.inventory.util.StockMath;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BatchServiceImpl implements BatchService {

    private static final Set<String> SORT_FIELDS = Set.of("id", "batchNumber", "expiryDate", "receivedDate",
            "quantityAvailable", "foodItemId", "qualityStatus");

    private final StockBatchRepository batchRepository;
    private final StockItemRepository itemRepository;
    private final StockMovementRepository movementRepository;

    @Override
    public PageResponse<BatchResponse> search(Long foodItemId, QualityStatus qualityStatus, Long supplierId,
            String q, Boolean expired, Integer expiringWithinDays, Boolean inStock, int page, int size,
            String sort, String dir) {
        LocalDate today = LocalDate.now();
        Specification<StockBatch> spec = (root, query, cb) -> cb.conjunction();
        if (foodItemId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("foodItemId"), foodItemId));
        }
        if (qualityStatus != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("qualityStatus"), qualityStatus));
        }
        if (supplierId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("supplierId"), supplierId));
        }
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("batchNumber")), like));
        }
        if (expired != null) {
            if (expired) {
                spec = spec.and((root, query, cb) -> cb.lessThan(root.<LocalDate>get("expiryDate"), today));
            } else {
                spec = spec.and((root, query, cb) -> cb.or(cb.isNull(root.get("expiryDate")),
                        cb.greaterThanOrEqualTo(root.<LocalDate>get("expiryDate"), today)));
            }
        }
        if (expiringWithinDays != null) {
            LocalDate limit = today.plusDays(Math.max(expiringWithinDays, 0));
            spec = spec.and((root, query, cb) -> cb.and(
                    cb.greaterThanOrEqualTo(root.<LocalDate>get("expiryDate"), today),
                    cb.lessThanOrEqualTo(root.<LocalDate>get("expiryDate"), limit)));
        }
        if (inStock != null) {
            if (inStock) {
                spec = spec.and((root, query, cb) -> cb.greaterThan(root.<BigDecimal>get("quantityAvailable"),
                        BigDecimal.ZERO));
            } else {
                spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(
                        root.<BigDecimal>get("quantityAvailable"), BigDecimal.ZERO));
            }
        }
        String sortField = SORT_FIELDS.contains(sort) ? sort : "expiryDate";
        Sort.Direction direction = "desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 1000),
                Sort.by(direction, sortField).and(Sort.by(Sort.Direction.ASC, "id")));
        Page<StockBatch> result = batchRepository.findAll(spec, pageable);
        return new PageResponse<>(toResponses(result.getContent(), today), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Override
    public BatchResponse get(Long id) {
        StockBatch b = batchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + id));
        return toResponses(List.of(b), LocalDate.now()).get(0);
    }

    @Override
    @Transactional
    public BatchResponse updateQualityStatus(Long id, QualityStatus status, String remarks) {
        if (status == null) {
            throw new InvalidRequestException("Quality status is required");
        }
        StockBatch b = batchRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + id));
        QualityStatus old = b.getQualityStatus();
        b.setQualityStatus(status);
        batchRepository.save(b);
        log.info("Batch {} quality status {} -> {} ({})", b.getBatchNumber(), old, status, remarks);
        return toResponses(List.of(b), LocalDate.now()).get(0);
    }

    @Override
    @Transactional
    public BatchResponse writeOff(Long id, WriteOffRequest request) {
        StockBatch b = batchRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + id));
        BigDecimal qty = request.getQuantity().setScale(3, RoundingMode.HALF_UP);
        BigDecimal free = StockMath.unreserved(b);
        if (qty.compareTo(free) > 0) {
            throw new BusinessException("Cannot write off " + qty + " from batch " + b.getBatchNumber()
                    + ": only " + free + " is on hand and not reserved");
        }
        b.setQuantityAvailable(b.getQuantityAvailable().subtract(qty));
        batchRepository.save(b);
        movementRepository.save(StockMovement.builder().batchId(b.getId()).foodItemId(b.getFoodItemId())
                .type(MovementType.WASTAGE).quantity(qty).referenceType("WASTAGE")
                .referenceId(request.getReferenceId()).createdAt(LocalDateTime.now())
                .remarks(request.getReason().trim()).build());
        log.info("Wrote off {} from batch {} ({})", qty, b.getBatchNumber(), request.getReason());
        return toResponses(List.of(b), LocalDate.now()).get(0);
    }

    @Override
    public List<BatchResponse> expiring(int days) {
        LocalDate today = LocalDate.now();
        List<StockBatch> list = batchRepository
                .findByExpiryDateBetweenAndQuantityAvailableGreaterThanOrderByExpiryDateAsc(today,
                        today.plusDays(Math.max(days, 0)), BigDecimal.ZERO);
        return toResponses(list, today);
    }

    @Override
    public List<BatchResponse> expired() {
        LocalDate today = LocalDate.now();
        List<StockBatch> list = batchRepository
                .findByExpiryDateBeforeAndQuantityAvailableGreaterThanOrderByExpiryDateAsc(today, BigDecimal.ZERO);
        return toResponses(list, today);
    }

    private List<BatchResponse> toResponses(List<StockBatch> batches, LocalDate today) {
        if (batches.isEmpty()) {
            return List.of();
        }
        Set<Long> ids = batches.stream().map(StockBatch::getFoodItemId).collect(Collectors.toSet());
        Map<Long, StockItem> items = namesFor(ids);
        return batches.stream().map(b -> {
            StockItem i = items.get(b.getFoodItemId());
            return BatchMapper.toResponse(b, i != null ? i.getFoodItemName() : "Food item #" + b.getFoodItemId(),
                    today);
        }).toList();
    }

    private Map<Long, StockItem> namesFor(Collection<Long> ids) {
        return itemRepository.findByFoodItemIdIn(ids).stream()
                .collect(Collectors.toMap(StockItem::getFoodItemId, Function.identity(), (a, b) -> a));
    }
}

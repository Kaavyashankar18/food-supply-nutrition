package com.foodsupply.inventory.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.foodsupply.inventory.dto.AllocationResponse;
import com.foodsupply.inventory.dto.AvailabilityRequest;
import com.foodsupply.inventory.dto.AvailabilityResponse;
import com.foodsupply.inventory.dto.BatchResponse;
import com.foodsupply.inventory.dto.ItemAvailability;
import com.foodsupply.inventory.dto.PageResponse;
import com.foodsupply.inventory.dto.QuantityRequest;
import com.foodsupply.inventory.dto.ReceiveStockRequest;
import com.foodsupply.inventory.dto.ReferenceRequest;
import com.foodsupply.inventory.dto.ReservationResponse;
import com.foodsupply.inventory.dto.ReservationResultResponse;
import com.foodsupply.inventory.dto.ReserveRequest;
import com.foodsupply.inventory.dto.ShortageDetail;
import com.foodsupply.inventory.dto.StockLevelResponse;
import com.foodsupply.inventory.dto.StockSummaryResponse;
import com.foodsupply.inventory.entity.MovementType;
import com.foodsupply.inventory.entity.QualityStatus;
import com.foodsupply.inventory.entity.Reservation;
import com.foodsupply.inventory.entity.ReservationStatus;
import com.foodsupply.inventory.entity.StockBatch;
import com.foodsupply.inventory.entity.StockItem;
import com.foodsupply.inventory.entity.StockMovement;
import com.foodsupply.inventory.exception.BusinessException;
import com.foodsupply.inventory.exception.InvalidRequestException;
import com.foodsupply.inventory.exception.ResourceNotFoundException;
import com.foodsupply.inventory.exception.ShortageException;
import com.foodsupply.inventory.mapper.BatchMapper;
import com.foodsupply.inventory.mapper.MovementMapper;
import com.foodsupply.inventory.repository.ReservationRepository;
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
public class StockServiceImpl implements StockService {

    public static final int EXPIRING_DAYS = 7;

    private final StockItemRepository itemRepository;
    private final StockBatchRepository batchRepository;
    private final StockMovementRepository movementRepository;
    private final ReservationRepository reservationRepository;

    // ------------------------------------------------------------------ levels

    @Override
    public PageResponse<StockLevelResponse> levels(String q, Boolean lowStock, int page, int size, String sort,
            String dir) {
        List<StockLevelResponse> all = computeLevels();
        String needle = q == null ? "" : q.trim().toLowerCase();
        List<StockLevelResponse> filtered = new ArrayList<>();
        for (StockLevelResponse l : all) {
            if (!needle.isEmpty() && !l.foodItemName().toLowerCase().contains(needle)) {
                continue;
            }
            if (lowStock != null && l.lowStock() != lowStock) {
                continue;
            }
            filtered.add(l);
        }
        Comparator<StockLevelResponse> cmp = switch (sort == null ? "" : sort) {
            case "onHand" -> Comparator.comparing((StockLevelResponse l) -> l.onHand());
            case "usable" -> Comparator.comparing((StockLevelResponse l) -> l.usable());
            case "minimumLevel" -> Comparator.comparing((StockLevelResponse l) -> l.minimumStockLevel());
            case "foodItemId" -> Comparator.comparing((StockLevelResponse l) -> l.foodItemId());
            default -> Comparator.comparing((StockLevelResponse l) -> l.foodItemName().toLowerCase());
        };
        if ("desc".equalsIgnoreCase(dir)) {
            cmp = cmp.reversed();
        }
        filtered.sort(cmp);

        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), 1000);
        int total = filtered.size();
        int from = Math.min(p * s, total);
        int to = Math.min(from + s, total);
        int totalPages = (int) Math.ceil(total / (double) s);
        return new PageResponse<>(new ArrayList<>(filtered.subList(from, to)), p, s, total, totalPages);
    }

    @Override
    public StockLevelResponse level(Long foodItemId) {
        StockItem item = itemRepository.findByFoodItemId(foodItemId)
                .orElseThrow(() -> new ResourceNotFoundException("No stock record for food item " + foodItemId));
        List<StockBatch> batches = batchRepository.findByFoodItemIdIn(List.of(foodItemId));
        return level(item, batches, LocalDate.now());
    }

    @Override
    public List<StockLevelResponse> lowStock() {
        return computeLevels().stream().filter(StockLevelResponse::lowStock)
                .sorted(Comparator.comparing((StockLevelResponse l) -> l.foodItemName().toLowerCase())).toList();
    }

    @Override
    @Transactional
    public StockLevelResponse setMinimumLevel(Long foodItemId, BigDecimal value) {
        if (value == null || value.signum() < 0) {
            throw new InvalidRequestException("Minimum stock level must be zero or more");
        }
        StockItem item = itemRepository.findByFoodItemId(foodItemId)
                .orElseThrow(() -> new ResourceNotFoundException("No stock record for food item " + foodItemId));
        item.setMinimumStockLevel(value.setScale(3, RoundingMode.HALF_UP));
        itemRepository.save(item);
        return level(item, batchRepository.findByFoodItemIdIn(List.of(foodItemId)), LocalDate.now());
    }

    private List<StockLevelResponse> computeLevels() {
        LocalDate today = LocalDate.now();
        Map<Long, List<StockBatch>> byItem = batchRepository.findAll().stream()
                .collect(Collectors.groupingBy(StockBatch::getFoodItemId));
        List<StockLevelResponse> result = new ArrayList<>();
        for (StockItem item : itemRepository.findAll()) {
            result.add(level(item, byItem.getOrDefault(item.getFoodItemId(), List.of()), today));
        }
        return result;
    }

    private StockLevelResponse level(StockItem item, List<StockBatch> batches, LocalDate today) {
        BigDecimal onHand = BigDecimal.ZERO;
        BigDecimal reserved = BigDecimal.ZERO;
        BigDecimal usable = BigDecimal.ZERO;
        BigDecimal expiringSoon = BigDecimal.ZERO;
        BigDecimal expired = BigDecimal.ZERO;
        BigDecimal quarantined = BigDecimal.ZERO;
        BigDecimal rejected = BigDecimal.ZERO;
        LocalDate soonLimit = today.plusDays(EXPIRING_DAYS);

        for (StockBatch b : batches) {
            onHand = onHand.add(b.getQuantityAvailable());
            reserved = reserved.add(b.getQuantityReserved());
            switch (StockMath.bucket(b, today)) {
                case USABLE -> {
                    BigDecimal u = StockMath.unreserved(b);
                    usable = usable.add(u);
                    if (b.getExpiryDate() != null && !b.getExpiryDate().isAfter(soonLimit)) {
                        expiringSoon = expiringSoon.add(u);
                    }
                }
                case EXPIRED -> expired = expired.add(b.getQuantityAvailable());
                case QUARANTINED -> quarantined = quarantined.add(b.getQuantityAvailable());
                case REJECTED -> rejected = rejected.add(b.getQuantityAvailable());
            }
        }
        BigDecimal min = item.getMinimumStockLevel() == null ? BigDecimal.ZERO : item.getMinimumStockLevel();
        boolean low = min.signum() > 0 && usable.compareTo(min) < 0;
        return new StockLevelResponse(item.getFoodItemId(), item.getFoodItemName(), item.getUnit(),
                item.getStorageLocation(), min, onHand, reserved, usable, expiringSoon, expired, quarantined,
                rejected, low);
    }

    // ------------------------------------------------------------------ receive

    @Override
    @Transactional
    public BatchResponse receive(ReceiveStockRequest r) {
        String batchNo = r.getBatchNumber().trim();
        if (batchRepository.existsByBatchNumberIgnoreCase(batchNo)) {
            throw new BusinessException("Batch number already exists: " + batchNo);
        }
        LocalDate today = LocalDate.now();
        LocalDate received = r.getReceivedDate() != null ? r.getReceivedDate() : today;
        if (r.getExpiryDate() != null && r.getExpiryDate().isBefore(received)) {
            throw new InvalidRequestException("Expiry date cannot be before the received date");
        }

        StockItem item = itemRepository.findByFoodItemId(r.getFoodItemId()).orElse(null);
        if (item == null) {
            item = itemRepository.save(StockItem.builder().foodItemId(r.getFoodItemId())
                    .foodItemName(r.getFoodItemName().trim()).unit(r.getUnit())
                    .minimumStockLevel(BigDecimal.ZERO).storageLocation(blankToNull(r.getStorageLocation()))
                    .build());
            log.info("Created stock item for food item {} ({})", item.getFoodItemId(), item.getFoodItemName());
        } else if (item.getUnit() != r.getUnit()) {
            throw new InvalidRequestException("Unit mismatch for food item " + r.getFoodItemId() + ": stock is kept in "
                    + item.getUnit() + " but request uses " + r.getUnit());
        }

        BigDecimal qty = r.getQuantity().setScale(3, RoundingMode.HALF_UP);
        StockBatch batch = batchRepository.save(StockBatch.builder().foodItemId(r.getFoodItemId())
                .batchNumber(batchNo).supplierId(r.getSupplierId()).quantityReceived(qty).quantityAvailable(qty)
                .quantityReserved(BigDecimal.ZERO).receivedDate(received).expiryDate(r.getExpiryDate())
                .storageLocation(blankToNull(r.getStorageLocation()) != null ? r.getStorageLocation().trim()
                        : item.getStorageLocation())
                .qualityStatus(QualityStatus.APPROVED).build());
        movementRepository.save(movement(batch, MovementType.RECEIPT, qty, "RECEIPT", batch.getId(),
                "Received batch " + batchNo, LocalDateTime.now()));
        log.info("Received {} of food item {} as batch {}", qty, r.getFoodItemId(), batchNo);
        return BatchMapper.toResponse(batch, item.getFoodItemName(), today);
    }

    // ------------------------------------------------------------------ availability

    @Override
    public AvailabilityResponse availability(AvailabilityRequest request) {
        Map<Long, BigDecimal> required = merge(request.getItems());
        LocalDate today = LocalDate.now();
        LocalDate until = request.getNeededUntil();
        Map<Long, StockItem> items = itemsById(required.keySet());
        Map<Long, List<StockBatch>> batches = batchRepository.findByFoodItemIdIn(required.keySet()).stream()
                .collect(Collectors.groupingBy(StockBatch::getFoodItemId));

        List<ItemAvailability> result = new ArrayList<>();
        boolean all = true;
        for (Map.Entry<Long, BigDecimal> e : required.entrySet()) {
            BigDecimal usable = BigDecimal.ZERO;
            BigDecimal expiringBefore = BigDecimal.ZERO;
            BigDecimal expired = BigDecimal.ZERO;
            BigDecimal quarantined = BigDecimal.ZERO;
            BigDecimal rejected = BigDecimal.ZERO;
            for (StockBatch b : batches.getOrDefault(e.getKey(), List.of())) {
                switch (StockMath.bucket(b, today)) {
                    case USABLE -> {
                        BigDecimal u = StockMath.unreserved(b);
                        usable = usable.add(u);
                        if (until != null && b.getExpiryDate() != null && b.getExpiryDate().isBefore(until)) {
                            expiringBefore = expiringBefore.add(u);
                        }
                    }
                    case EXPIRED -> expired = expired.add(b.getQuantityAvailable());
                    case QUARANTINED -> quarantined = quarantined.add(b.getQuantityAvailable());
                    case REJECTED -> rejected = rejected.add(b.getQuantityAvailable());
                }
            }
            BigDecimal req = e.getValue();
            BigDecimal shortage = req.subtract(usable).max(BigDecimal.ZERO);
            boolean sufficient = usable.compareTo(req) >= 0;
            all = all && sufficient;
            StockItem item = items.get(e.getKey());
            result.add(new ItemAvailability(e.getKey(), nameOf(items, e.getKey()),
                    item == null ? null : item.getUnit(), req, usable, shortage, expiringBefore, expired,
                    quarantined, rejected, usable.subtract(expiringBefore), sufficient));
        }
        return new AvailabilityResponse(until, all, result);
    }

    // ------------------------------------------------------------------ reserve / release / dispatch

    @Override
    @Transactional
    public ReservationResultResponse reserve(ReserveRequest r) {
        String refType = normRef(r.getReferenceType());
        Long refId = r.getReferenceId();
        if (reservationRepository.existsByReferenceTypeAndReferenceIdAndStatus(refType, refId,
                ReservationStatus.ACTIVE)) {
            throw new BusinessException("An active reservation already exists for " + refType + " " + refId);
        }
        Map<Long, BigDecimal> required = merge(r.getItems());
        Map<Long, StockItem> items = itemsById(required.keySet());
        LocalDate today = LocalDate.now();

        // 1) plan: lock candidate batches, check every item (all-or-nothing)
        Map<Long, List<StockBatch>> candidates = new LinkedHashMap<>();
        List<ShortageDetail> shortages = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> e : required.entrySet()) {
            List<StockBatch> list = new ArrayList<>(
                    batchRepository.findAllocatable(e.getKey(), QualityStatus.APPROVED, today));
            list.sort(StockMath.FEFO);
            BigDecimal usable = BigDecimal.ZERO;
            for (StockBatch b : list) {
                usable = usable.add(StockMath.unreserved(b));
            }
            if (usable.compareTo(e.getValue()) < 0) {
                shortages.add(new ShortageDetail(e.getKey(), nameOf(items, e.getKey()), e.getValue(), usable,
                        e.getValue().subtract(usable)));
            }
            candidates.put(e.getKey(), list);
        }
        if (!shortages.isEmpty()) {
            String msg = shortages.stream()
                    .map(s -> s.foodItemName() + " (need " + s.requiredQuantity() + ", usable " + s.usableQuantity()
                            + ")")
                    .collect(Collectors.joining("; ", "Insufficient usable stock, nothing reserved: ", ""));
            throw new ShortageException(msg, shortages);
        }

        // 2) allocate FEFO
        List<AllocationResponse> allocations = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (Map.Entry<Long, BigDecimal> e : required.entrySet()) {
            BigDecimal remaining = e.getValue();
            for (StockBatch b : candidates.get(e.getKey())) {
                if (remaining.signum() <= 0) {
                    break;
                }
                BigDecimal free = StockMath.unreserved(b);
                if (free.signum() <= 0) {
                    continue;
                }
                BigDecimal take = free.min(remaining);
                b.setQuantityReserved(b.getQuantityReserved().add(take));
                remaining = remaining.subtract(take);
                reservationRepository.save(Reservation.builder().referenceType(refType).referenceId(refId)
                        .batchId(b.getId()).foodItemId(b.getFoodItemId()).quantity(take)
                        .status(ReservationStatus.ACTIVE).build());
                movementRepository.save(movement(b, MovementType.RESERVE, take, refType, refId, null, now));
                allocations.add(new AllocationResponse(b.getFoodItemId(), nameOf(items, b.getFoodItemId()),
                        b.getId(), b.getBatchNumber(), b.getExpiryDate(), take));
            }
        }
        log.info("Reserved stock for {} {} ({} allocations)", refType, refId, allocations.size());
        return new ReservationResultResponse(refType, refId, "RESERVED", allocations);
    }

    @Override
    @Transactional
    public ReservationResultResponse release(ReferenceRequest r) {
        return finish(r, false);
    }

    @Override
    @Transactional
    public ReservationResultResponse dispatch(ReferenceRequest r) {
        return finish(r, true);
    }

    private ReservationResultResponse finish(ReferenceRequest r, boolean dispatch) {
        String refType = normRef(r.getReferenceType());
        Long refId = r.getReferenceId();
        List<Reservation> active = reservationRepository.findByReferenceTypeAndReferenceIdAndStatus(refType, refId,
                ReservationStatus.ACTIVE);
        if (active.isEmpty()) {
            throw new ResourceNotFoundException("No active reservation for " + refType + " " + refId);
        }
        Set<Long> batchIds = active.stream().map(Reservation::getBatchId).collect(Collectors.toSet());
        Map<Long, StockBatch> batches = batchRepository.findAllByIdForUpdate(batchIds).stream()
                .collect(Collectors.toMap(StockBatch::getId, Function.identity()));
        Set<Long> foodIds = active.stream().map(Reservation::getFoodItemId).collect(Collectors.toSet());
        Map<Long, StockItem> items = itemsById(foodIds);
        LocalDateTime now = LocalDateTime.now();

        List<AllocationResponse> allocations = new ArrayList<>();
        for (Reservation res : active) {
            StockBatch b = batches.get(res.getBatchId());
            if (b == null) {
                throw new BusinessException("Batch " + res.getBatchId() + " of the reservation no longer exists");
            }
            BigDecimal qty = res.getQuantity();
            if (b.getQuantityReserved().compareTo(qty) < 0) {
                throw new BusinessException("Stock inconsistency on batch " + b.getBatchNumber());
            }
            b.setQuantityReserved(b.getQuantityReserved().subtract(qty));
            if (dispatch) {
                if (b.getQuantityAvailable().compareTo(qty) < 0) {
                    throw new BusinessException("Not enough stock on hand in batch " + b.getBatchNumber());
                }
                b.setQuantityAvailable(b.getQuantityAvailable().subtract(qty));
                res.setStatus(ReservationStatus.DISPATCHED);
                movementRepository.save(movement(b, MovementType.DISPATCH, qty, refType, refId, null, now));
            } else {
                res.setStatus(ReservationStatus.RELEASED);
                movementRepository.save(movement(b, MovementType.RELEASE, qty, refType, refId, null, now));
            }
            reservationRepository.save(res);
            allocations.add(new AllocationResponse(b.getFoodItemId(), nameOf(items, b.getFoodItemId()), b.getId(),
                    b.getBatchNumber(), b.getExpiryDate(), qty));
        }
        String action = dispatch ? "DISPATCHED" : "RELEASED";
        log.info("{} stock for {} {} ({} allocations)", action, refType, refId, allocations.size());
        return new ReservationResultResponse(refType, refId, action, allocations);
    }

    @Override
    public List<ReservationResponse> reservations(String referenceType, Long referenceId) {
        return reservationRepository
                .findByReferenceTypeAndReferenceIdOrderByIdAsc(normRef(referenceType), referenceId).stream()
                .map(MovementMapper::toResponse).toList();
    }

    // ------------------------------------------------------------------ summary

    @Override
    public StockSummaryResponse summary() {
        LocalDate today = LocalDate.now();
        LocalDate soonLimit = today.plusDays(EXPIRING_DAYS);
        List<StockBatch> batches = batchRepository.findAll();
        Map<Long, StockItem> items = itemRepository.findAll().stream()
                .collect(Collectors.toMap(StockItem::getFoodItemId, Function.identity()));

        long expiring = 0;
        long expired = 0;
        long quarantined = 0;
        long rejected = 0;
        for (StockBatch b : batches) {
            if (b.getQuantityAvailable().signum() <= 0) {
                continue;
            }
            switch (StockMath.bucket(b, today)) {
                case USABLE -> {
                    if (b.getExpiryDate() != null && !b.getExpiryDate().isAfter(soonLimit)) {
                        expiring++;
                    }
                }
                case EXPIRED -> expired++;
                case QUARANTINED -> quarantined++;
                case REJECTED -> rejected++;
            }
        }

        Map<Long, List<StockBatch>> byItem = batches.stream()
                .collect(Collectors.groupingBy(StockBatch::getFoodItemId));
        Map<String, BigDecimal> byUnit = new LinkedHashMap<>();
        BigDecimal totalUsable = BigDecimal.ZERO;
        long outOfStock = 0;
        long low = 0;
        for (StockItem item : items.values()) {
            StockLevelResponse l = level(item, byItem.getOrDefault(item.getFoodItemId(), List.of()), today);
            totalUsable = totalUsable.add(l.usable());
            byUnit.merge(item.getUnit().name(), l.usable(), BigDecimal::add);
            if (l.usable().signum() <= 0) {
                outOfStock++;
            }
            if (l.lowStock()) {
                low++;
            }
        }
        return new StockSummaryResponse(items.size(), batches.size(), totalUsable, byUnit, outOfStock, low,
                expiring, expired, quarantined, rejected,
                reservationRepository.countByStatus(ReservationStatus.ACTIVE), EXPIRING_DAYS);
    }

    // ------------------------------------------------------------------ helpers

    private static Map<Long, BigDecimal> merge(List<QuantityRequest> items) {
        Map<Long, BigDecimal> merged = new LinkedHashMap<>();
        for (QuantityRequest q : items) {
            merged.merge(q.getFoodItemId(), q.getRequiredQuantity().setScale(3, RoundingMode.HALF_UP),
                    BigDecimal::add);
        }
        return merged;
    }

    private Map<Long, StockItem> itemsById(Collection<Long> foodItemIds) {
        return itemRepository.findByFoodItemIdIn(foodItemIds).stream()
                .collect(Collectors.toMap(StockItem::getFoodItemId, Function.identity(), (a, b) -> a));
    }

    private static String nameOf(Map<Long, StockItem> items, Long foodItemId) {
        StockItem i = items.get(foodItemId);
        return i != null ? i.getFoodItemName() : "Food item #" + foodItemId;
    }

    private static String normRef(String referenceType) {
        return referenceType.trim().toUpperCase();
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static StockMovement movement(StockBatch b, MovementType type, BigDecimal qty, String refType,
            Long refId, String remarks, LocalDateTime now) {
        return StockMovement.builder().batchId(b.getId()).foodItemId(b.getFoodItemId()).type(type).quantity(qty)
                .referenceType(refType).referenceId(refId).createdAt(now).remarks(remarks).build();
    }
}

package com.foodsupply.procurement.service;

import com.foodsupply.procurement.client.InventoryClient;
import com.foodsupply.procurement.dto.PageResponse;
import com.foodsupply.procurement.dto.PurchaseOrderInput;
import com.foodsupply.procurement.dto.PurchaseOrderResponse;
import com.foodsupply.procurement.dto.ReceiveInput;
import com.foodsupply.procurement.dto.ReceiveLineInput;
import com.foodsupply.procurement.dto.ReceiveStockRequest;
import com.foodsupply.procurement.dto.UnitPriceInput;
import com.foodsupply.procurement.entity.ProcurementRequest;
import com.foodsupply.procurement.entity.PurchaseOrder;
import com.foodsupply.procurement.entity.PurchaseOrderItem;
import com.foodsupply.procurement.entity.PurchaseOrderStatus;
import com.foodsupply.procurement.entity.RequestItem;
import com.foodsupply.procurement.entity.RequestStatus;
import com.foodsupply.procurement.entity.Supplier;
import com.foodsupply.procurement.entity.SupplierStatus;
import com.foodsupply.procurement.exception.BusinessException;
import com.foodsupply.procurement.exception.DownstreamServiceException;
import com.foodsupply.procurement.exception.InvalidRequestException;
import com.foodsupply.procurement.exception.ResourceNotFoundException;
import com.foodsupply.procurement.mapper.ProcurementMapper;
import com.foodsupply.procurement.repository.ProcurementRequestRepository;
import com.foodsupply.procurement.repository.PurchaseOrderRepository;
import com.foodsupply.procurement.repository.SupplierRepository;
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
import java.time.LocalDate;
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
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private static final Set<String> SORT_FIELDS =
            Set.of("id", "poNumber", "orderDate", "expectedDeliveryDate", "status", "supplierName");

    private final PurchaseOrderRepository orderRepository;
    private final ProcurementRequestRepository requestRepository;
    private final SupplierRepository supplierRepository;
    private final InventoryClient inventoryClient;

    @Override
    public PageResponse<PurchaseOrderResponse> list(String q, PurchaseOrderStatus status, Long supplierId,
                                                    Long requestId, int page, int size, String sort, String dir) {
        Specification<PurchaseOrder> spec = (root, query, cb) -> cb.conjunction();
        if (q != null && !q.isBlank()) {
            String pattern = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.<String>get("poNumber")), pattern),
                    cb.like(cb.lower(root.<String>get("supplierName")), pattern),
                    cb.like(cb.lower(root.<String>get("requestNumber")), pattern)));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (supplierId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("supplierId"), supplierId));
        }
        if (requestId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("requestId"), requestId));
        }
        Pageable pageable = PageUtil.of(page, size, sort, dir, SORT_FIELDS, "id", Sort.Direction.DESC);
        Page<PurchaseOrderResponse> result =
                orderRepository.findAll(spec, pageable).map(ProcurementMapper::toOrderResponse);
        return PageResponse.of(result);
    }

    @Override
    public PurchaseOrderResponse get(Long id) {
        return ProcurementMapper.toOrderResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public PurchaseOrderResponse create(PurchaseOrderInput input) {
        ProcurementRequest request = requestRepository.findById(input.getRequestId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Procurement request not found with id " + input.getRequestId()));
        if (request.getStatus() != RequestStatus.APPROVED) {
            throw new BusinessException("Request " + request.getRequestNumber() + " is " + request.getStatus()
                    + "; purchase orders can only be created from APPROVED requests");
        }
        Supplier supplier = supplierRepository.findById(input.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Supplier not found with id " + input.getSupplierId()));
        if (supplier.getStatus() != SupplierStatus.ACTIVE) {
            throw new BusinessException("Supplier '" + supplier.getName() + "' is INACTIVE");
        }
        LocalDate today = LocalDate.now();
        if (input.getExpectedDeliveryDate().isBefore(today)) {
            throw new InvalidRequestException("expectedDeliveryDate cannot be in the past");
        }

        Set<Long> requestItemFoodIds = new HashSet<>();
        for (RequestItem ri : request.getItems()) {
            requestItemFoodIds.add(ri.getFoodItemId());
        }
        Map<Long, BigDecimal> prices = new HashMap<>();
        List<UnitPriceInput> priceInputs = input.getUnitPrices();
        if (priceInputs != null) {
            for (UnitPriceInput up : priceInputs) {
                if (!requestItemFoodIds.contains(up.getFoodItemId())) {
                    throw new InvalidRequestException("Food item " + up.getFoodItemId()
                            + " is not part of request " + request.getRequestNumber());
                }
                prices.put(up.getFoodItemId(), up.getUnitPrice());
            }
        }

        PurchaseOrder po = PurchaseOrder.builder()
                .poNumber(nextPoNumber(today.getYear()))
                .supplierId(supplier.getId())
                .supplierName(supplier.getName())
                .requestId(request.getId())
                .requestNumber(request.getRequestNumber())
                .orderDate(today)
                .expectedDeliveryDate(input.getExpectedDeliveryDate())
                .status(PurchaseOrderStatus.CREATED)
                .build();
        for (RequestItem ri : request.getItems()) {
            BigDecimal price = prices.get(ri.getFoodItemId());
            if (price == null) {
                price = ri.getEstimatedUnitPrice();
            }
            if (price == null) {
                throw new InvalidRequestException("Unit price is required for '" + ri.getFoodItemName()
                        + "' (no estimated price on the request)");
            }
            po.addItem(PurchaseOrderItem.builder()
                    .foodItemId(ri.getFoodItemId())
                    .foodItemName(ri.getFoodItemName())
                    .unit(ri.getUnit())
                    .orderedQty(ri.getQuantity())
                    .receivedQty(BigDecimal.ZERO)
                    .unitPrice(price)
                    .build());
        }
        request.setStatus(RequestStatus.ORDERED);

        PurchaseOrder saved = orderRepository.save(po);
        log.info("Created purchase order {} for request {} with supplier {}", saved.getPoNumber(),
                request.getRequestNumber(), supplier.getName());
        return ProcurementMapper.toOrderResponse(saved);
    }

    /**
     * Receives goods line by line through inventory-service. Lines that were accepted by inventory stay saved
     * even if a later line fails (noRollbackFor), so this service never disagrees with the stock ledger.
     */
    @Override
    @Transactional(noRollbackFor = {BusinessException.class, DownstreamServiceException.class,
            InvalidRequestException.class})
    public PurchaseOrderResponse receive(Long id, ReceiveInput input) {
        PurchaseOrder po = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found with id " + id));
        PurchaseOrderStatus current = po.getStatus();
        if (current != PurchaseOrderStatus.CREATED && current != PurchaseOrderStatus.SENT
                && current != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new BusinessException("Purchase order " + po.getPoNumber() + " is " + current
                    + "; goods can only be received for CREATED, SENT or PARTIALLY_RECEIVED orders");
        }

        Map<Long, PurchaseOrderItem> itemsById = new HashMap<>();
        for (PurchaseOrderItem item : po.getItems()) {
            itemsById.put(item.getId(), item);
        }

        // 1) validate everything before touching inventory
        Map<Long, BigDecimal> incoming = new HashMap<>();
        Set<String> batchNumbers = new HashSet<>();
        for (ReceiveLineInput line : input.getItems()) {
            PurchaseOrderItem item = itemsById.get(line.getItemId());
            if (item == null) {
                throw new InvalidRequestException("Item " + line.getItemId()
                        + " does not belong to purchase order " + po.getPoNumber());
            }
            if (!batchNumbers.add(line.getBatchNumber().trim().toUpperCase())) {
                throw new InvalidRequestException("Batch number '" + line.getBatchNumber().trim()
                        + "' is used on more than one line");
            }
            incoming.merge(item.getId(), line.getQuantityReceived(), BigDecimal::add);
        }
        for (Map.Entry<Long, BigDecimal> entry : incoming.entrySet()) {
            PurchaseOrderItem item = itemsById.get(entry.getKey());
            BigDecimal remaining = item.getOrderedQty().subtract(item.getReceivedQty());
            if (entry.getValue().compareTo(remaining) > 0) {
                throw new BusinessException(String.format(
                        "Over-receipt for '%s': ordered %s, already received %s, attempted %s",
                        item.getFoodItemName(), item.getOrderedQty().stripTrailingZeros().toPlainString(),
                        item.getReceivedQty().stripTrailingZeros().toPlainString(),
                        entry.getValue().stripTrailingZeros().toPlainString()));
            }
        }

        // 2) post each line to inventory
        String today = LocalDate.now().toString();
        int processed = 0;
        RuntimeException failure = null;
        ReceiveLineInput failedLine = null;
        for (ReceiveLineInput line : input.getItems()) {
            PurchaseOrderItem item = itemsById.get(line.getItemId());
            ReceiveStockRequest body = new ReceiveStockRequest(item.getFoodItemId(), item.getFoodItemName(),
                    item.getUnit().name(), line.getBatchNumber().trim(), po.getSupplierId(),
                    line.getQuantityReceived(), today,
                    line.getExpiryDate() == null ? null : line.getExpiryDate().toString(),
                    line.getStorageLocation());
            try {
                inventoryClient.receiveStock(body);
            } catch (RuntimeException ex) {
                failure = ex;
                failedLine = line;
                break;
            }
            item.setReceivedQty(item.getReceivedQty().add(line.getQuantityReceived()));
            processed++;
        }

        // 3) update status of PO and request for what was actually received
        refreshStatus(po);
        log.info("Purchase order {}: {} of {} line(s) received, status {}", po.getPoNumber(), processed,
                input.getItems().size(), po.getStatus());

        if (failure != null) {
            throw partialFailure(failure, processed, input.getItems().size(), failedLine);
        }
        return ProcurementMapper.toOrderResponse(po);
    }

    @Override
    @Transactional
    public PurchaseOrderResponse updateStatus(Long id, PurchaseOrderStatus status) {
        if (status != PurchaseOrderStatus.SENT && status != PurchaseOrderStatus.CANCELLED) {
            throw new InvalidRequestException("Only SENT or CANCELLED can be set manually; "
                    + "PARTIALLY_RECEIVED and RECEIVED are set by receiving goods");
        }
        PurchaseOrder po = findOrThrow(id);
        if (status == PurchaseOrderStatus.SENT) {
            if (po.getStatus() != PurchaseOrderStatus.CREATED) {
                throw new BusinessException("Purchase order " + po.getPoNumber() + " is " + po.getStatus()
                        + "; only CREATED orders can be marked SENT");
            }
            po.setStatus(PurchaseOrderStatus.SENT);
        } else {
            if (po.getStatus() != PurchaseOrderStatus.CREATED && po.getStatus() != PurchaseOrderStatus.SENT) {
                throw new BusinessException("Purchase order " + po.getPoNumber() + " is " + po.getStatus()
                        + "; only CREATED or SENT orders can be cancelled");
            }
            po.setStatus(PurchaseOrderStatus.CANCELLED);
            Optional<ProcurementRequest> request = requestRepository.findById(po.getRequestId());
            if (request.isPresent() && request.get().getStatus() == RequestStatus.ORDERED) {
                request.get().setStatus(RequestStatus.APPROVED);
            }
        }
        log.info("Purchase order {} status -> {}", po.getPoNumber(), po.getStatus());
        return ProcurementMapper.toOrderResponse(po);
    }

    // ------------------------------------------------------------------ helpers

    private void refreshStatus(PurchaseOrder po) {
        boolean allReceived = true;
        boolean anyReceived = false;
        for (PurchaseOrderItem item : po.getItems()) {
            if (item.getReceivedQty().compareTo(item.getOrderedQty()) < 0) {
                allReceived = false;
            }
            if (item.getReceivedQty().compareTo(BigDecimal.ZERO) > 0) {
                anyReceived = true;
            }
        }
        if (allReceived) {
            po.setStatus(PurchaseOrderStatus.RECEIVED);
            Optional<ProcurementRequest> request = requestRepository.findById(po.getRequestId());
            if (request.isPresent()) {
                request.get().setStatus(RequestStatus.COMPLETED);
            }
        } else if (anyReceived) {
            po.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        }
    }

    private RuntimeException partialFailure(RuntimeException failure, int processed, int total,
                                            ReceiveLineInput failedLine) {
        String message = "Batch '" + failedLine.getBatchNumber().trim() + "' failed after " + processed + " of "
                + total + " line(s) were received and saved: " + failure.getMessage();
        if (failure instanceof DownstreamServiceException) {
            return new DownstreamServiceException(message);
        }
        if (failure instanceof InvalidRequestException) {
            return new InvalidRequestException(message);
        }
        if (failure instanceof BusinessException || failure instanceof ResourceNotFoundException) {
            return new BusinessException(message);
        }
        return new DownstreamServiceException(message);
    }

    private PurchaseOrder findOrThrow(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found with id " + id));
    }

    private String nextPoNumber(int year) {
        String prefix = "PO-" + year + "-";
        Optional<PurchaseOrder> last = orderRepository.findTopByPoNumberStartingWithOrderByPoNumberDesc(prefix);
        int next = 1;
        if (last.isPresent()) {
            String number = last.get().getPoNumber();
            try {
                next = Integer.parseInt(number.substring(number.lastIndexOf('-') + 1)) + 1;
            } catch (NumberFormatException e) {
                log.warn("Unexpected PO number format: {}", number);
            }
        }
        return String.format("%s%04d", prefix, next);
    }
}

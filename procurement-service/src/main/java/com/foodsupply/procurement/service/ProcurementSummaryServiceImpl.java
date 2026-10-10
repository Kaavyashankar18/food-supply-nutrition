package com.foodsupply.procurement.service;

import com.foodsupply.procurement.dto.ProcurementSummary;
import com.foodsupply.procurement.entity.ProcurementRequest;
import com.foodsupply.procurement.entity.PurchaseOrder;
import com.foodsupply.procurement.entity.PurchaseOrderItem;
import com.foodsupply.procurement.entity.PurchaseOrderStatus;
import com.foodsupply.procurement.entity.RequestStatus;
import com.foodsupply.procurement.entity.SupplierStatus;
import com.foodsupply.procurement.repository.ProcurementRequestRepository;
import com.foodsupply.procurement.repository.PurchaseOrderRepository;
import com.foodsupply.procurement.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProcurementSummaryServiceImpl implements ProcurementSummaryService {

    private final ProcurementRequestRepository requestRepository;
    private final PurchaseOrderRepository orderRepository;
    private final SupplierRepository supplierRepository;

    @Override
    public ProcurementSummary summary() {
        Map<String, Long> requestsByStatus = new LinkedHashMap<>();
        long totalRequests = 0;
        for (RequestStatus s : RequestStatus.values()) {
            long count = requestRepository.countByStatus(s);
            requestsByStatus.put(s.name(), count);
            totalRequests += count;
        }
        Map<String, Long> ordersByStatus = new LinkedHashMap<>();
        long totalOrders = 0;
        for (PurchaseOrderStatus s : PurchaseOrderStatus.values()) {
            long count = orderRepository.countByStatus(s);
            ordersByStatus.put(s.name(), count);
            totalOrders += count;
        }
        long pending = requestsByStatus.get(RequestStatus.DRAFT.name())
                + requestsByStatus.get(RequestStatus.SUBMITTED.name());

        BigDecimal estimated = BigDecimal.ZERO;
        for (ProcurementRequest r : requestRepository.findAll()) {
            if (r.getStatus() != RequestStatus.REJECTED && r.getEstimatedCost() != null) {
                estimated = estimated.add(r.getEstimatedCost());
            }
        }
        BigDecimal orderValue = BigDecimal.ZERO;
        for (PurchaseOrder po : orderRepository.findAll()) {
            if (po.getStatus() == PurchaseOrderStatus.CANCELLED) {
                continue;
            }
            for (PurchaseOrderItem item : po.getItems()) {
                orderValue = orderValue.add(item.getOrderedQty().multiply(item.getUnitPrice()));
            }
        }
        long totalSuppliers = supplierRepository.count();
        long activeSuppliers = supplierRepository.countByStatus(SupplierStatus.ACTIVE);

        return new ProcurementSummary(totalRequests, pending, requestsByStatus, totalOrders, ordersByStatus,
                estimated.setScale(2, RoundingMode.HALF_UP), orderValue.setScale(2, RoundingMode.HALF_UP),
                totalSuppliers, activeSuppliers);
    }
}

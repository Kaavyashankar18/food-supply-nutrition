package com.foodsupply.procurement.dto;

import com.foodsupply.procurement.entity.PurchaseOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PurchaseOrderResponse(Long id, String poNumber, Long supplierId, String supplierName,
                                    Long requestId, String requestNumber, LocalDate orderDate,
                                    LocalDate expectedDeliveryDate, PurchaseOrderStatus status,
                                    BigDecimal totalAmount, List<PurchaseOrderItemResponse> items) {
}

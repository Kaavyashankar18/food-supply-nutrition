package com.foodsupply.procurement.dto;

import java.math.BigDecimal;
import java.util.Map;

public record ProcurementSummary(long totalRequests, long pendingRequests, Map<String, Long> requestsByStatus,
                                 long totalPurchaseOrders, Map<String, Long> purchaseOrdersByStatus,
                                 BigDecimal totalEstimatedCost, BigDecimal totalOrderValue,
                                 long totalSuppliers, long activeSuppliers) {
}

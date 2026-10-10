package com.foodsupply.procurement.dto;

import java.math.BigDecimal;

/** Body of inventory-service POST /api/v1/stock/receive (dates sent as yyyy-MM-dd strings). */
public record ReceiveStockRequest(Long foodItemId, String foodItemName, String unit, String batchNumber,
                                  Long supplierId, BigDecimal quantity, String receivedDate, String expiryDate,
                                  String storageLocation) {
}

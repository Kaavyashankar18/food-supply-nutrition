package com.foodsupply.procurement.dto;

import com.foodsupply.procurement.entity.FoodUnit;

import java.math.BigDecimal;

public record PurchaseOrderItemResponse(Long id, Long foodItemId, String foodItemName, FoodUnit unit,
                                        BigDecimal orderedQty, BigDecimal receivedQty, BigDecimal pendingQty,
                                        BigDecimal unitPrice, BigDecimal lineTotal) {
}

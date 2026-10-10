package com.foodsupply.procurement.dto;

import com.foodsupply.procurement.entity.FoodUnit;

import java.math.BigDecimal;

public record RequestItemResponse(Long id, Long foodItemId, String foodItemName, FoodUnit unit,
                                  BigDecimal quantity, BigDecimal estimatedUnitPrice, BigDecimal estimatedCost) {
}

package com.foodsupply.distribution.dto;

import com.foodsupply.distribution.entity.FoodUnit;

import java.math.BigDecimal;
import java.util.List;

public record ItemResponse(Long foodItemId, String foodItemName, FoodUnit unit, BigDecimal plannedQty,
                           BigDecimal dispatchedQty, BigDecimal deliveredQty, List<AllocationResponse> allocations) {
}

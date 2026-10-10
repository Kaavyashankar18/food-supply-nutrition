package com.foodsupply.inventory.dto;

import java.math.BigDecimal;

import com.foodsupply.inventory.entity.FoodUnit;

public record ItemAvailability(Long foodItemId, String foodItemName, FoodUnit unit, BigDecimal requiredQuantity,
        BigDecimal usableQuantity, BigDecimal shortage, BigDecimal expiringSoonQuantity,
        BigDecimal expiredQuantity, BigDecimal quarantinedQuantity, BigDecimal rejectedQuantity,
        BigDecimal usableByNeededDate, boolean sufficient) {
}

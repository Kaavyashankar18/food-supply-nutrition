package com.foodsupply.inventory.dto;

import java.math.BigDecimal;

public record ShortageDetail(Long foodItemId, String foodItemName, BigDecimal requiredQuantity,
        BigDecimal usableQuantity, BigDecimal shortage) {
}

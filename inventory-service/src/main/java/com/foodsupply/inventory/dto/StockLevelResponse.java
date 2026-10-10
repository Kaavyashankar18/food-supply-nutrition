package com.foodsupply.inventory.dto;

import java.math.BigDecimal;

import com.foodsupply.inventory.entity.FoodUnit;

public record StockLevelResponse(Long foodItemId, String foodItemName, FoodUnit unit, String storageLocation,
        BigDecimal minimumStockLevel, BigDecimal onHand, BigDecimal reserved, BigDecimal usable,
        BigDecimal expiringSoon, BigDecimal expired, BigDecimal quarantined, BigDecimal rejected,
        boolean lowStock) {
}

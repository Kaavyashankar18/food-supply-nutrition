package com.foodsupply.inventory.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Dashboard figures. Quantities are in each item's own unit, so usableByUnit groups them per unit;
 * totalUsableQuantity is the plain sum across units and is only a rough indicator.
 */
public record StockSummaryResponse(long totalItems, long totalBatches, BigDecimal totalUsableQuantity,
        Map<String, BigDecimal> usableByUnit, long outOfStockItems, long lowStockItems, long expiringBatches,
        long expiredBatches, long quarantinedBatches, long rejectedBatches, long activeReservations,
        int expiringWithinDays) {
}

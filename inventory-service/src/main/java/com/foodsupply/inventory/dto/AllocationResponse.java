package com.foodsupply.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AllocationResponse(Long foodItemId, String foodItemName, Long batchId, String batchNumber,
        LocalDate expiryDate, BigDecimal quantity) {
}

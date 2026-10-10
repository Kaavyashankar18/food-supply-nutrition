package com.foodsupply.quality.dto;

import com.foodsupply.quality.entity.FoodUnit;
import com.foodsupply.quality.entity.WastageReason;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WastageResponse(
        Long id,
        Long batchId,
        String batchNumber,
        Long foodItemId,
        String foodItemName,
        FoodUnit unit,
        BigDecimal quantity,
        WastageReason reason,
        String disposalMethod,
        LocalDate recordedOn,
        String recordedBy,
        String remarks) {
}

package com.foodsupply.distribution.dto;

import com.foodsupply.distribution.entity.FoodUnit;
import com.foodsupply.distribution.entity.RecordStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecordResponse(Long id, Long planId, String planNumber, Long beneficiaryId, String beneficiaryName,
                             Long foodItemId, String foodItemName, FoodUnit unit, BigDecimal quantity,
                             LocalDate deliveredOn, RecordStatus status) {
}

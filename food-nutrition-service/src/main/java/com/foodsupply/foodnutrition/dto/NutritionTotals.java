package com.foodsupply.foodnutrition.dto;

import java.math.BigDecimal;

public record NutritionTotals(BigDecimal calories, BigDecimal protein, BigDecimal carbs, BigDecimal fat,
        BigDecimal fibre) {
}

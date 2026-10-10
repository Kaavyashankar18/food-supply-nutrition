package com.foodsupply.foodnutrition.dto;

import java.util.List;

public record MealCalculationResponse(int persons, int days, List<MealItemResult> items,
        NutritionTotals nutritionPerPersonPerDay) {
}

package com.foodsupply.foodnutrition.dto;

import java.math.BigDecimal;

import com.foodsupply.foodnutrition.entity.FoodUnit;

public record MealItemResult(Long foodItemId, String foodItemName, FoodUnit unit, BigDecimal quantityPerPerson,
        BigDecimal totalQuantity, NutritionTotals nutritionPerPersonPerDay) {
}

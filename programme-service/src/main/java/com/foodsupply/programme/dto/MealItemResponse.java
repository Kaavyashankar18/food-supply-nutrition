package com.foodsupply.programme.dto;

import com.foodsupply.programme.entity.FoodUnit;

import java.math.BigDecimal;

public record MealItemResponse(Long foodItemId, String foodItemName, FoodUnit unit, BigDecimal quantityPerPerson) {
}

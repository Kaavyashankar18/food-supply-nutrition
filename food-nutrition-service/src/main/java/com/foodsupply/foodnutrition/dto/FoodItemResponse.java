package com.foodsupply.foodnutrition.dto;

import java.math.BigDecimal;

import com.foodsupply.foodnutrition.entity.FoodUnit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class FoodItemResponse {

    private final Long id;
    private final String name;
    private final Long categoryId;
    private final String categoryName;
    private final FoodUnit unit;
    private final BigDecimal gramsPerUnit;
    private final BigDecimal caloriesPer100g;
    private final BigDecimal proteinPer100g;
    private final BigDecimal carbsPer100g;
    private final BigDecimal fatPer100g;
    private final BigDecimal fibrePer100g;
    private final String allergens;
    private final String storageRequirement;
    private final BigDecimal recommendedQtyPerPersonPerDay;
    private final boolean active;
}

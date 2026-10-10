package com.foodsupply.procurement.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Subset of food-nutrition-service FoodItemResponse. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FoodItemInfo(Long id, String name, String unit, Boolean active) {
}

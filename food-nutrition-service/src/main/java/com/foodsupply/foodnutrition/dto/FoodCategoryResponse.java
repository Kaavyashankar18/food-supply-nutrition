package com.foodsupply.foodnutrition.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class FoodCategoryResponse {

    private final Long id;
    private final String name;
    private final long itemCount;
}

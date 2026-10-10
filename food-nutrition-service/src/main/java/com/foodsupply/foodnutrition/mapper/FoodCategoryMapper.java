package com.foodsupply.foodnutrition.mapper;

import com.foodsupply.foodnutrition.dto.FoodCategoryResponse;
import com.foodsupply.foodnutrition.entity.FoodCategory;

public final class FoodCategoryMapper {

    private FoodCategoryMapper() {
    }

    public static FoodCategoryResponse toResponse(FoodCategory c, long itemCount) {
        return FoodCategoryResponse.builder().id(c.getId()).name(c.getName()).itemCount(itemCount).build();
    }
}

package com.foodsupply.foodnutrition.service;

import java.util.List;

import com.foodsupply.foodnutrition.dto.FoodItemRequest;
import com.foodsupply.foodnutrition.dto.FoodItemResponse;
import com.foodsupply.foodnutrition.dto.FoodStatsResponse;
import com.foodsupply.foodnutrition.dto.PageResponse;
import com.foodsupply.foodnutrition.entity.FoodUnit;

public interface FoodItemService {

    PageResponse<FoodItemResponse> search(String q, Long categoryId, FoodUnit unit, Boolean active, int page,
            int size, String sort, String dir);

    FoodItemResponse get(Long id);

    List<FoodItemResponse> getByIds(List<Long> ids);

    FoodItemResponse create(FoodItemRequest request);

    FoodItemResponse update(Long id, FoodItemRequest request);

    /** Soft delete: marks the item inactive, because other services reference it by id. */
    void deactivate(Long id);

    FoodItemResponse setActive(Long id, boolean active);

    FoodStatsResponse stats();
}

package com.foodsupply.programme.service;

import com.foodsupply.programme.dto.MealPlanRequest;
import com.foodsupply.programme.dto.MealPlanResponse;

import java.util.List;

public interface MealPlanService {

    List<MealPlanResponse> list(Long programmeId);

    MealPlanResponse create(Long programmeId, MealPlanRequest request);

    MealPlanResponse update(Long programmeId, Long mealPlanId, MealPlanRequest request);

    void delete(Long programmeId, Long mealPlanId);
}

package com.foodsupply.programme.dto;

import com.foodsupply.programme.entity.MealType;

import java.time.DayOfWeek;
import java.util.List;

public record MealPlanResponse(Long id, Long programmeId, DayOfWeek dayOfWeek, MealType mealType,
                               List<MealItemResponse> items) {
}

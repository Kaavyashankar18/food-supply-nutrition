package com.foodsupply.programme.dto;

import com.foodsupply.programme.entity.MealType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class MealPlanRequest {

    @NotNull(message = "dayOfWeek is required (MONDAY..SUNDAY)")
    private DayOfWeek dayOfWeek;

    @NotNull(message = "mealType is required")
    private MealType mealType;

    @NotEmpty(message = "at least one item is required")
    @Valid
    private List<MealItemRequest> items;
}

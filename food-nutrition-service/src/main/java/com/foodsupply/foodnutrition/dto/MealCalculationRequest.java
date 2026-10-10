package com.foodsupply.foodnutrition.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MealCalculationRequest {

    @NotNull(message = "persons is required")
    @Min(value = 1, message = "persons must be at least 1")
    @Max(value = 10000000, message = "persons is too large")
    private Integer persons;

    @NotNull(message = "days is required")
    @Min(value = 1, message = "days must be at least 1")
    @Max(value = 3650, message = "days is too large")
    private Integer days;

    @NotEmpty(message = "At least one item is required")
    @Valid
    private List<MealItemInput> items;
}

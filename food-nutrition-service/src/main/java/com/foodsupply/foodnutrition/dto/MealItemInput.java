package com.foodsupply.foodnutrition.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MealItemInput {

    @NotNull(message = "foodItemId is required")
    private Long foodItemId;

    /** Per person per day, in the food item's own unit. */
    @NotNull(message = "quantityPerPerson is required")
    @DecimalMin(value = "0.001", message = "quantityPerPerson must be greater than 0")
    private BigDecimal quantityPerPerson;
}

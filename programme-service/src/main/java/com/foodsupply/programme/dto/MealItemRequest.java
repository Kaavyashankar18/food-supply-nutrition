package com.foodsupply.programme.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class MealItemRequest {

    @NotNull(message = "foodItemId is required")
    private Long foodItemId;

    @NotNull(message = "quantityPerPerson is required")
    @DecimalMin(value = "0.001", message = "quantityPerPerson must be at least 0.001")
    @Digits(integer = 9, fraction = 3, message = "quantityPerPerson allows at most 3 decimals")
    private BigDecimal quantityPerPerson;
}

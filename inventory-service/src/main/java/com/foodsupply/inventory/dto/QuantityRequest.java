package com.foodsupply.inventory.dto;

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
public class QuantityRequest {

    @NotNull(message = "foodItemId is required")
    private Long foodItemId;

    @NotNull(message = "requiredQuantity is required")
    @DecimalMin(value = "0.001", message = "requiredQuantity must be greater than 0")
    private BigDecimal requiredQuantity;
}

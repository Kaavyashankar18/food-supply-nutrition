package com.foodsupply.distribution.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class DeliveredItem {

    @NotNull(message = "foodItemId is required")
    private Long foodItemId;

    @NotNull(message = "deliveredQuantity is required")
    @DecimalMin(value = "0", message = "deliveredQuantity must not be negative")
    private BigDecimal deliveredQuantity;
}

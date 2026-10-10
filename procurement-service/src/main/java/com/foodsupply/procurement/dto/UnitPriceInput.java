package com.foodsupply.procurement.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnitPriceInput {

    @NotNull(message = "foodItemId is required")
    private Long foodItemId;

    @NotNull(message = "unitPrice is required")
    @DecimalMin(value = "0.01", message = "unitPrice must be greater than zero")
    @Digits(integer = 10, fraction = 2, message = "unitPrice allows up to 2 decimal places")
    private BigDecimal unitPrice;
}

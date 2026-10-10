package com.foodsupply.procurement.dto;

import com.foodsupply.procurement.entity.FoodUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
public class RequestItemInput {

    @NotNull(message = "foodItemId is required")
    @Positive(message = "foodItemId must be positive")
    private Long foodItemId;

    /** Optional: when both name and unit are supplied, food-nutrition-service is not called for this line. */
    @Size(max = 150, message = "Food item name must be at most 150 characters")
    private String foodItemName;

    private FoodUnit unit;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.001", message = "Quantity must be greater than zero")
    @Digits(integer = 9, fraction = 3, message = "Quantity allows up to 9 integer and 3 decimal digits")
    private BigDecimal quantity;

    @DecimalMin(value = "0.00", message = "Estimated unit price cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Price allows up to 2 decimal places")
    private BigDecimal estimatedUnitPrice;
}

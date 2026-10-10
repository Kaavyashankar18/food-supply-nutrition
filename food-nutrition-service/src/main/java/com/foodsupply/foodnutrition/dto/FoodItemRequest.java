package com.foodsupply.foodnutrition.dto;

import java.math.BigDecimal;

import com.foodsupply.foodnutrition.entity.FoodUnit;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodItemRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 120, message = "Name must be at most 120 characters")
    private String name;

    @NotNull(message = "Category is required")
    private Long categoryId;

    @NotNull(message = "Unit is required (KG, LITRE or PIECE)")
    private FoodUnit unit;

    @NotNull(message = "Grams per unit is required")
    @DecimalMin(value = "0.001", message = "Grams per unit must be greater than 0")
    @DecimalMax(value = "100000", message = "Grams per unit is too large")
    private BigDecimal gramsPerUnit;

    @NotNull(message = "Calories per 100g is required")
    @DecimalMin(value = "0", message = "Calories cannot be negative")
    @DecimalMax(value = "1000", message = "Calories per 100g cannot exceed 1000")
    private BigDecimal caloriesPer100g;

    @NotNull(message = "Protein per 100g is required")
    @DecimalMin(value = "0", message = "Protein cannot be negative")
    @DecimalMax(value = "100", message = "Protein per 100g cannot exceed 100")
    private BigDecimal proteinPer100g;

    @NotNull(message = "Carbs per 100g is required")
    @DecimalMin(value = "0", message = "Carbs cannot be negative")
    @DecimalMax(value = "100", message = "Carbs per 100g cannot exceed 100")
    private BigDecimal carbsPer100g;

    @NotNull(message = "Fat per 100g is required")
    @DecimalMin(value = "0", message = "Fat cannot be negative")
    @DecimalMax(value = "100", message = "Fat per 100g cannot exceed 100")
    private BigDecimal fatPer100g;

    @NotNull(message = "Fibre per 100g is required")
    @DecimalMin(value = "0", message = "Fibre cannot be negative")
    @DecimalMax(value = "100", message = "Fibre per 100g cannot exceed 100")
    private BigDecimal fibrePer100g;

    @Size(max = 255, message = "Allergens must be at most 255 characters")
    private String allergens;

    @Size(max = 255, message = "Storage requirement must be at most 255 characters")
    private String storageRequirement;

    @NotNull(message = "Recommended quantity per person per day is required")
    @DecimalMin(value = "0.001", message = "Recommended quantity must be greater than 0")
    @DecimalMax(value = "100", message = "Recommended quantity is too large")
    private BigDecimal recommendedQtyPerPersonPerDay;

    /** Optional; defaults to true when omitted. */
    private Boolean active;
}

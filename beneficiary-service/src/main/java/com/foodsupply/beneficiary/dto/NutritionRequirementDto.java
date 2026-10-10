package com.foodsupply.beneficiary.dto;

import com.foodsupply.beneficiary.entity.BeneficiaryCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
public class NutritionRequirementDto {

    private Long id;

    @NotNull(message = "Category is required")
    private BeneficiaryCategory category;

    @NotNull(message = "Daily calories is required")
    @Min(value = 1, message = "Calories must be positive")
    private Integer dailyCalories;

    @NotNull(message = "Daily protein is required")
    @DecimalMin(value = "0.0", message = "Protein cannot be negative")
    private BigDecimal dailyProteinG;

    @DecimalMin("0.0")
    private BigDecimal dailyCarbsG;

    @DecimalMin("0.0")
    private BigDecimal dailyFatG;

    @DecimalMin("0.0")
    private BigDecimal dailyFibreG;

    @Size(max = 255)
    private String notes;
}

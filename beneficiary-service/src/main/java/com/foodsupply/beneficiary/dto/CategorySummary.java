package com.foodsupply.beneficiary.dto;

import com.foodsupply.beneficiary.entity.BeneficiaryCategory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * People covered per category (a school of 500 counts as 500) plus that category's daily requirement.
 * Used by programme-service to size a weekly plan without fetching every record.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategorySummary {
    private BeneficiaryCategory category;
    private long count;
    private NutritionRequirementDto requirement;
}

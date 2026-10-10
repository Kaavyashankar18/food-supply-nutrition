package com.foodsupply.programme.dto;

import com.foodsupply.programme.entity.FoodUnit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RequirementsResponse(Long programmeId, String programmeName, LocalDate from, LocalDate to,
                                   int days, int servingDays, int enrolledPeople,
                                   List<RequiredItem> items, List<RequiredItem> shortages,
                                   boolean allSufficient, Nutrition nutrition) {

    public record RequiredItem(Long foodItemId, String foodItemName, FoodUnit unit, BigDecimal quantityPerPerson,
                               BigDecimal totalQuantity, BigDecimal usableQuantity, BigDecimal shortage,
                               BigDecimal expiringSoonQuantity, BigDecimal usableByNeededDate, boolean sufficient) {
    }

    public record NutritionValues(BigDecimal calories, BigDecimal protein, BigDecimal carbs, BigDecimal fat,
                                  BigDecimal fibre) {
    }

    /** perPersonPerDay = average per serving day; required/percentMet are null when the programme has no target category. */
    public record Nutrition(String category, NutritionValues perPersonPerDay, NutritionValues required,
                            NutritionValues percentMet) {
    }
}

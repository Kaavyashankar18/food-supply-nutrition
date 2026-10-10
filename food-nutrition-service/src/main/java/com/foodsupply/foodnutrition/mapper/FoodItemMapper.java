package com.foodsupply.foodnutrition.mapper;

import com.foodsupply.foodnutrition.dto.FoodItemRequest;
import com.foodsupply.foodnutrition.dto.FoodItemResponse;
import com.foodsupply.foodnutrition.entity.FoodCategory;
import com.foodsupply.foodnutrition.entity.FoodItem;

public final class FoodItemMapper {

    private FoodItemMapper() {
    }

    public static FoodItem toEntity(FoodItemRequest r, FoodCategory category) {
        FoodItem item = new FoodItem();
        apply(item, r, category);
        return item;
    }

    public static void apply(FoodItem item, FoodItemRequest r, FoodCategory category) {
        item.setName(r.getName().trim());
        item.setCategory(category);
        item.setUnit(r.getUnit());
        item.setGramsPerUnit(r.getGramsPerUnit());
        item.setCaloriesPer100g(r.getCaloriesPer100g());
        item.setProteinPer100g(r.getProteinPer100g());
        item.setCarbsPer100g(r.getCarbsPer100g());
        item.setFatPer100g(r.getFatPer100g());
        item.setFibrePer100g(r.getFibrePer100g());
        item.setAllergens(blankToNull(r.getAllergens()));
        item.setStorageRequirement(blankToNull(r.getStorageRequirement()));
        item.setRecommendedQtyPerPersonPerDay(r.getRecommendedQtyPerPersonPerDay());
        if (r.getActive() != null) {
            item.setActive(r.getActive());
        }
    }

    /** Must be called while the persistence session is open (category is lazy). */
    public static FoodItemResponse toResponse(FoodItem i) {
        return FoodItemResponse.builder()
                .id(i.getId())
                .name(i.getName())
                .categoryId(i.getCategory().getId())
                .categoryName(i.getCategory().getName())
                .unit(i.getUnit())
                .gramsPerUnit(i.getGramsPerUnit())
                .caloriesPer100g(i.getCaloriesPer100g())
                .proteinPer100g(i.getProteinPer100g())
                .carbsPer100g(i.getCarbsPer100g())
                .fatPer100g(i.getFatPer100g())
                .fibrePer100g(i.getFibrePer100g())
                .allergens(i.getAllergens())
                .storageRequirement(i.getStorageRequirement())
                .recommendedQtyPerPersonPerDay(i.getRecommendedQtyPerPersonPerDay())
                .active(i.isActive())
                .build();
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}

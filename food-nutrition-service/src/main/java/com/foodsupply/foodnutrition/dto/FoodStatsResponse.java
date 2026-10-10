package com.foodsupply.foodnutrition.dto;

import java.util.Map;

public record FoodStatsResponse(long totalItems, long activeItems, long inactiveItems, long categories,
        Map<String, Long> itemsPerCategory) {
}

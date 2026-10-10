package com.foodsupply.quality.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** The part of inventory-service's StockLevelResponse that this service needs. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StockInfo(Long foodItemId, String foodItemName, String unit) {
}

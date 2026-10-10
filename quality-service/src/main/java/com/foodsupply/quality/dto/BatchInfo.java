package com.foodsupply.quality.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** The part of inventory-service's BatchResponse that this service needs. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BatchInfo(Long id, Long foodItemId, String foodItemName, String batchNumber, String qualityStatus) {
}

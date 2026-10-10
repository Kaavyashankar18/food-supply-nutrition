package com.foodsupply.procurement.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/** Subset of inventory-service BatchResponse. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BatchInfo(Long id, Long foodItemId, String batchNumber, BigDecimal quantityReceived,
                        String qualityStatus) {
}

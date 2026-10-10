package com.foodsupply.quality.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

/** Body for inventory-service POST /api/v1/batches/{id}/write-off. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WriteOffRequest(BigDecimal quantity, String reason, Long referenceId) {
}

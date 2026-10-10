package com.foodsupply.inventory.dto;

import java.math.BigDecimal;

import com.foodsupply.inventory.entity.ReservationStatus;

public record ReservationResponse(Long id, String referenceType, Long referenceId, Long batchId, Long foodItemId,
        BigDecimal quantity, ReservationStatus status) {
}

package com.foodsupply.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.foodsupply.inventory.entity.MovementType;

public record MovementResponse(Long id, Long batchId, Long foodItemId, MovementType type, BigDecimal quantity,
        String referenceType, Long referenceId, LocalDateTime createdAt, String remarks) {
}

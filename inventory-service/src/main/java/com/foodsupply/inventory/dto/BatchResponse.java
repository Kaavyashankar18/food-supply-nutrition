package com.foodsupply.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.foodsupply.inventory.entity.QualityStatus;

public record BatchResponse(Long id, Long foodItemId, String foodItemName, String batchNumber, Long supplierId,
        BigDecimal quantityReceived, BigDecimal quantityAvailable, BigDecimal quantityReserved,
        BigDecimal usableQuantity, LocalDate receivedDate, LocalDate expiryDate, Integer daysToExpiry,
        boolean expired, String storageLocation, QualityStatus qualityStatus) {
}

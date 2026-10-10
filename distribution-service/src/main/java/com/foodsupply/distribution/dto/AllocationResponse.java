package com.foodsupply.distribution.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AllocationResponse(Long batchId, String batchNumber, LocalDate expiryDate, BigDecimal quantity) {
}

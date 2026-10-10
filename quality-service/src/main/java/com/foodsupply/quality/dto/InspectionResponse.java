package com.foodsupply.quality.dto;

import com.foodsupply.quality.entity.InspectionResult;
import com.foodsupply.quality.entity.QualityStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record InspectionResponse(
        Long id,
        LocalDate inspectionDate,
        Long foodItemId,
        String foodItemName,
        Long batchId,
        String batchNumber,
        String inspector,
        InspectionResult result,
        QualityStatus qualityStatus,
        String remarks,
        LocalDateTime createdAt) {
}

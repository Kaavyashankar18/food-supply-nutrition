package com.foodsupply.quality.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Dashboard summary. totalWastageQuantity adds raw numbers across units, so use
 * wastageByUnit for accurate figures.
 */
public record QualitySummaryResponse(
        long totalInspections,
        Map<String, Long> inspectionsByResult,
        Map<String, Long> inspectionsByQualityStatus,
        long openActions,
        long overdueActions,
        Map<String, Long> actionsByStatus,
        long totalWastageRecords,
        BigDecimal totalWastageQuantity,
        Map<String, BigDecimal> wastageByUnit) {
}

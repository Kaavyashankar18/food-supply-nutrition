package com.foodsupply.quality.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record WastageSummaryResponse(
        LocalDate from,
        LocalDate to,
        long totalRecords,
        BigDecimal totalQuantity,
        Map<String, BigDecimal> quantityByUnit,
        List<ReasonSummary> byReason,
        List<MonthSummary> byMonth) {

    public record ReasonSummary(String reason, long records, BigDecimal quantity) {
    }

    public record MonthSummary(String month, long records, BigDecimal quantity) {
    }
}

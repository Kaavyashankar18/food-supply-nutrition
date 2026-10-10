package com.foodsupply.reporting.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ReportResponse(
        String report,
        String title,
        LocalDateTime generatedAt,
        Map<String, Object> filters,
        List<String> columns,
        List<Map<String, Object>> rows,
        Map<String, Object> totals,
        List<String> unavailable) {
}

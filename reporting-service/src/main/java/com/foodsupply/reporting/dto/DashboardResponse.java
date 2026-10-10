package com.foodsupply.reporting.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record DashboardResponse(
        LocalDateTime generatedAt,
        Map<String, Object> cards,
        Map<String, Map<String, Object>> charts,
        List<String> unavailable) {
}

package com.foodsupply.distribution.dto;

import java.util.Map;

public record DistributionSummary(long totalPlans, Map<String, Long> byStatus, long pendingPlans, long deliveredPlans,
                                  long totalCentres, long activeCentres, long totalRecords) {
}

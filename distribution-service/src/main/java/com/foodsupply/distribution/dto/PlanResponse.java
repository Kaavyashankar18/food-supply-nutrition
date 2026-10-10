package com.foodsupply.distribution.dto;

import com.foodsupply.distribution.entity.PlanStatus;

import java.time.LocalDate;
import java.util.List;

public record PlanResponse(Long id, String planNumber, Long programmeId, String programmeName, Long centreId,
                           String centreName, LocalDate startDate, LocalDate endDate, PlanStatus status,
                           String remarks, LocalDate createdOn, LocalDate dispatchedOn, LocalDate deliveredOn,
                           int enrolledPeople, List<ItemResponse> items, List<TargetResponse> beneficiaries) {
}

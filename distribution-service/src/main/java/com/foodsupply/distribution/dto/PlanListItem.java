package com.foodsupply.distribution.dto;

import com.foodsupply.distribution.entity.PlanStatus;

import java.time.LocalDate;

public record PlanListItem(Long id, String planNumber, Long programmeId, String programmeName, Long centreId,
                           String centreName, LocalDate startDate, LocalDate endDate, PlanStatus status,
                           int enrolledPeople, int itemCount) {
}

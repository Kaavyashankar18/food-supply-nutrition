package com.foodsupply.quality.dto;

import com.foodsupply.quality.entity.ActionStatus;

import java.time.LocalDate;

public record ActionResponse(
        Long id,
        Long inspectionId,
        Long wastageId,
        String description,
        String assignedTo,
        LocalDate dueDate,
        ActionStatus status,
        LocalDate createdOn,
        LocalDate completedOn,
        boolean overdue) {
}

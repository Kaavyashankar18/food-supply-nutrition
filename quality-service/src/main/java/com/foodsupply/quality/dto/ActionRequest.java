package com.foodsupply.quality.dto;

import com.foodsupply.quality.entity.ActionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActionRequest {

    /** At least one of inspectionId / wastageId is required on create. */
    private Long inspectionId;

    private Long wastageId;

    @NotBlank(message = "description is required")
    @Size(max = 1000, message = "description must be at most 1000 characters")
    private String description;

    @NotBlank(message = "assignedTo is required")
    @Size(max = 100, message = "assignedTo must be at most 100 characters")
    private String assignedTo;

    @NotNull(message = "dueDate is required")
    private LocalDate dueDate;

    /** Optional, defaults to OPEN on create. */
    private ActionStatus status;
}

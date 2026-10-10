package com.foodsupply.distribution.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Used for /generate (preview; centreId optional) and for creating a plan (centreId required). */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlanRequest {

    @NotNull(message = "programmeId is required")
    private Long programmeId;

    @NotNull(message = "startDate is required")
    private LocalDate startDate;

    @NotNull(message = "endDate is required")
    private LocalDate endDate;

    private Long centreId;

    @Size(max = 500, message = "remarks must be at most 500 characters")
    private String remarks;
}

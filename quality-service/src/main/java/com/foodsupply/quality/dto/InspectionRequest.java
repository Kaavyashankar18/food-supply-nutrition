package com.foodsupply.quality.dto;

import com.foodsupply.quality.entity.InspectionResult;
import com.foodsupply.quality.entity.QualityStatus;
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
public class InspectionRequest {

    /** Defaults to today. */
    private LocalDate inspectionDate;

    @NotNull(message = "batchId is required")
    private Long batchId;

    @NotBlank(message = "inspector is required")
    @Size(max = 100, message = "inspector must be at most 100 characters")
    private String inspector;

    @NotNull(message = "result is required (PASS, FAIL or CONDITIONAL)")
    private InspectionResult result;

    /**
     * Status to apply to the batch in inventory. Optional: PASS -> APPROVED,
     * FAIL -> REJECTED, CONDITIONAL -> QUARANTINED when omitted.
     */
    private QualityStatus qualityStatus;

    @Size(max = 1000, message = "remarks must be at most 1000 characters")
    private String remarks;
}

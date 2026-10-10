package com.foodsupply.procurement.dto;

import com.foodsupply.procurement.entity.RequestSource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcurementRequestInput {

    /** Defaults to MANUAL. distribution-service sends SHORTAGE. */
    private RequestSource source;

    @Size(max = 500, message = "Reason must be at most 500 characters")
    private String reason;

    @NotBlank(message = "requestedBy is required")
    @Size(max = 100, message = "requestedBy must be at most 100 characters")
    private String requestedBy;

    private LocalDate needByDate;

    /** If true the request is created directly in SUBMITTED status (create only). Default false = DRAFT. */
    private Boolean submit;

    @NotEmpty(message = "At least one item is required")
    @Valid
    private List<RequestItemInput> items;
}

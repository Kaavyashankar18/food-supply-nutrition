package com.foodsupply.programme.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ParticipantRequest {

    @NotNull(message = "beneficiaryId is required")
    private Long beneficiaryId;
}

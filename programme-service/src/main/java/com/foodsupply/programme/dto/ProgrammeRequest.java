package com.foodsupply.programme.dto;

import com.foodsupply.programme.entity.BeneficiaryCategory;
import com.foodsupply.programme.entity.ProgrammeStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProgrammeRequest {

    @NotBlank(message = "name is required")
    @Size(max = 150, message = "name must be at most 150 characters")
    private String name;

    @Size(max = 60, message = "type must be at most 60 characters")
    private String type;

    @Size(max = 1000, message = "objectives must be at most 1000 characters")
    private String objectives;

    @NotNull(message = "startDate is required")
    private LocalDate startDate;

    @NotNull(message = "endDate is required")
    private LocalDate endDate;

    private BeneficiaryCategory targetCategory;

    private ProgrammeStatus status;
}

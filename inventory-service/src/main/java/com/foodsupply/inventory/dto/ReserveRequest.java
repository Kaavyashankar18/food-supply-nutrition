package com.foodsupply.inventory.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReserveRequest {

    @NotBlank(message = "referenceType is required")
    @Size(max = 40, message = "referenceType must be at most 40 characters")
    private String referenceType;

    @NotNull(message = "referenceId is required")
    private Long referenceId;

    @NotEmpty(message = "At least one item is required")
    @Valid
    private List<QuantityRequest> items;
}

package com.foodsupply.inventory.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityRequest {

    @NotEmpty(message = "At least one item is required")
    @Valid
    private List<QuantityRequest> items;

    /** Date until which the food is needed; used to flag stock that expires before then. Optional. */
    private LocalDate neededUntil;
}

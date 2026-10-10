package com.foodsupply.distribution.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/** items empty/omitted = everything delivered in full; if given, lines not listed count as 0 delivered. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class DeliveryRequest {

    @Valid
    private List<DeliveredItem> items;

    @Size(max = 500, message = "remarks must be at most 500 characters")
    private String remarks;

    private LocalDate deliveredOn;
}

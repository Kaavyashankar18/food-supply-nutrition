package com.foodsupply.quality.dto;

import com.foodsupply.quality.entity.FoodUnit;
import com.foodsupply.quality.entity.WastageReason;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WastageRequest {

    @NotNull(message = "batchId is required")
    private Long batchId;

    @NotNull(message = "quantity is required")
    @DecimalMin(value = "0.001", message = "quantity must be greater than zero")
    private BigDecimal quantity;

    @NotNull(message = "reason is required (EXPIRED, DAMAGED, SPOILED, CONTAMINATED or OTHER)")
    private WastageReason reason;

    @Size(max = 150, message = "disposalMethod must be at most 150 characters")
    private String disposalMethod;

    /** Defaults to today. */
    private LocalDate recordedOn;

    @NotBlank(message = "recordedBy is required")
    @Size(max = 100, message = "recordedBy must be at most 100 characters")
    private String recordedBy;

    /** Optional. When omitted the unit is looked up in inventory-service. */
    private FoodUnit unit;

    @Size(max = 1000, message = "remarks must be at most 1000 characters")
    private String remarks;
}

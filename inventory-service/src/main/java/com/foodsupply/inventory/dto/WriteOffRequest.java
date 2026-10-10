package com.foodsupply.inventory.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
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
public class WriteOffRequest {

    @NotNull(message = "quantity is required")
    @DecimalMin(value = "0.001", message = "quantity must be greater than 0")
    private BigDecimal quantity;

    @NotBlank(message = "reason is required")
    @Size(max = 200, message = "reason must be at most 200 characters")
    private String reason;

    /** Optional id of the wastage record in quality-service. */
    private Long referenceId;
}

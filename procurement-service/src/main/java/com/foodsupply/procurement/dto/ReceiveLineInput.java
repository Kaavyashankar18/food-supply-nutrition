package com.foodsupply.procurement.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
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
public class ReceiveLineInput {

    /** Id of the purchase order item (PurchaseOrderItemResponse.id). */
    @NotNull(message = "itemId is required")
    private Long itemId;

    @NotNull(message = "quantityReceived is required")
    @DecimalMin(value = "0.001", message = "quantityReceived must be greater than zero")
    @Digits(integer = 9, fraction = 3, message = "quantityReceived allows up to 3 decimal places")
    private BigDecimal quantityReceived;

    @NotBlank(message = "batchNumber is required")
    @Size(max = 60, message = "batchNumber must be at most 60 characters")
    private String batchNumber;

    private LocalDate expiryDate;

    @Size(max = 100, message = "storageLocation must be at most 100 characters")
    private String storageLocation;
}

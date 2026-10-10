package com.foodsupply.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.foodsupply.inventory.entity.FoodUnit;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceiveStockRequest {

    @NotNull(message = "foodItemId is required")
    private Long foodItemId;

    @NotBlank(message = "foodItemName is required")
    @Size(max = 120, message = "foodItemName must be at most 120 characters")
    private String foodItemName;

    @NotNull(message = "unit is required (KG, LITRE or PIECE)")
    private FoodUnit unit;

    @NotBlank(message = "batchNumber is required")
    @Size(max = 60, message = "batchNumber must be at most 60 characters")
    private String batchNumber;

    private Long supplierId;

    @NotNull(message = "quantity is required")
    @DecimalMin(value = "0.001", message = "quantity must be greater than 0")
    private BigDecimal quantity;

    /** Defaults to today. */
    private LocalDate receivedDate;

    /** Optional; null means the batch does not expire. */
    private LocalDate expiryDate;

    @Size(max = 100, message = "storageLocation must be at most 100 characters")
    private String storageLocation;
}

package com.foodsupply.procurement.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
public class PurchaseOrderInput {

    @NotNull(message = "requestId is required")
    private Long requestId;

    @NotNull(message = "supplierId is required")
    private Long supplierId;

    @NotNull(message = "expectedDeliveryDate is required")
    private LocalDate expectedDeliveryDate;

    /** Optional per-item prices; items without a price here fall back to the request's estimatedUnitPrice. */
    @Valid
    private List<UnitPriceInput> unitPrices;
}

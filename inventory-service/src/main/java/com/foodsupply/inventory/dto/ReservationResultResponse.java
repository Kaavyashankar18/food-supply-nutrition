package com.foodsupply.inventory.dto;

import java.util.List;

public record ReservationResultResponse(String referenceType, Long referenceId, String action,
        List<AllocationResponse> allocations) {
}

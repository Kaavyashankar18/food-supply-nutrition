package com.foodsupply.inventory.mapper;

import com.foodsupply.inventory.dto.MovementResponse;
import com.foodsupply.inventory.dto.ReservationResponse;
import com.foodsupply.inventory.entity.Reservation;
import com.foodsupply.inventory.entity.StockMovement;

public final class MovementMapper {

    private MovementMapper() {
    }

    public static MovementResponse toResponse(StockMovement m) {
        return new MovementResponse(m.getId(), m.getBatchId(), m.getFoodItemId(), m.getType(), m.getQuantity(),
                m.getReferenceType(), m.getReferenceId(), m.getCreatedAt(), m.getRemarks());
    }

    public static ReservationResponse toResponse(Reservation r) {
        return new ReservationResponse(r.getId(), r.getReferenceType(), r.getReferenceId(), r.getBatchId(),
                r.getFoodItemId(), r.getQuantity(), r.getStatus());
    }
}

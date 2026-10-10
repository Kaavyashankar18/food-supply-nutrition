package com.foodsupply.inventory.service;

import java.time.LocalDate;

import com.foodsupply.inventory.dto.MovementResponse;
import com.foodsupply.inventory.dto.PageResponse;
import com.foodsupply.inventory.entity.MovementType;

public interface MovementService {

    PageResponse<MovementResponse> search(Long foodItemId, Long batchId, MovementType type, String referenceType,
            Long referenceId, LocalDate from, LocalDate to, int page, int size, String sort, String dir);
}

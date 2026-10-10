package com.foodsupply.inventory.exception;

import java.time.LocalDateTime;
import java.util.List;

import com.foodsupply.inventory.dto.ShortageDetail;

/** Same fields as ErrorResponse plus the list of short items (HTTP 409 on reserve). */
public record ShortageErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path,
        List<ShortageDetail> shortages) {
}

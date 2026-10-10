package com.foodsupply.distribution.exception;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public record ShortageErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path,
                                    JsonNode shortages) {
}

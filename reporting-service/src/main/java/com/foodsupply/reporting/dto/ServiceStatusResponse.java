package com.foodsupply.reporting.dto;

public record ServiceStatusResponse(String service, String url, boolean up, String message) {
}

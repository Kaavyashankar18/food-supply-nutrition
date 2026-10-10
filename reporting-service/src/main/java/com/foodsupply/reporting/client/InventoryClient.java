package com.foodsupply.reporting.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InventoryClient extends BaseClient {

    public InventoryClient(@Value("${services.inventory.url}") String baseUrl) {
        super("inventory-service", baseUrl);
    }

    public JsonNode summary() {
        return get("/api/v1/stock/summary");
    }

    public JsonNode stock() {
        return get("/api/v1/stock", params("size", PAGE_SIZE));
    }

    public JsonNode lowStock() {
        return get("/api/v1/alerts/low-stock");
    }

    public JsonNode expiring(int days) {
        return get("/api/v1/alerts/expiring", params("days", days));
    }

    public JsonNode expired() {
        return get("/api/v1/alerts/expired");
    }
}

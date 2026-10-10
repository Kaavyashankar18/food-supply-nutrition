package com.foodsupply.reporting.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ProcurementClient extends BaseClient {

    public ProcurementClient(@Value("${services.procurement.url}") String baseUrl) {
        super("procurement-service", baseUrl);
    }

    public JsonNode summary() {
        return get("/api/v1/procurement/summary");
    }

    public JsonNode requests() {
        return get("/api/v1/procurement-requests", params("size", PAGE_SIZE));
    }

    public JsonNode purchaseOrders() {
        return get("/api/v1/purchase-orders", params("size", PAGE_SIZE));
    }
}

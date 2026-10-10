package com.foodsupply.reporting.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DistributionClient extends BaseClient {

    public DistributionClient(@Value("${services.distribution.url}") String baseUrl) {
        super("distribution-service", baseUrl);
    }

    public JsonNode summary() {
        return get("/api/v1/distribution-plans/summary");
    }

    public JsonNode plans() {
        return get("/api/v1/distribution-plans", params("size", PAGE_SIZE));
    }
}

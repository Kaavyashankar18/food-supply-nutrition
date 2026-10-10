package com.foodsupply.reporting.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class QualityClient extends BaseClient {

    public QualityClient(@Value("${services.quality.url}") String baseUrl) {
        super("quality-service", baseUrl);
    }

    public JsonNode summary() {
        return get("/api/v1/quality/summary");
    }

    public JsonNode wastage() {
        return get("/api/v1/wastage", params("size", PAGE_SIZE));
    }

    public JsonNode inspections() {
        return get("/api/v1/inspections", params("size", PAGE_SIZE));
    }

    public JsonNode correctiveActions() {
        return get("/api/v1/corrective-actions", params("size", PAGE_SIZE));
    }
}

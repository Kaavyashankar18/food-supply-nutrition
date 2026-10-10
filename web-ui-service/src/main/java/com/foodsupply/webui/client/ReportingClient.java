package com.foodsupply.webui.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ReportingClient {

    private final BackendClient api;

    public ReportingClient(@Value("${services.reporting.url}") String baseUrl) {
        this.api = new BackendClient("reporting-service", baseUrl);
    }

    public Map<String, Object> dashboard() {
        return api.get("/api/v1/reports/dashboard", new ParameterizedTypeReference<Map<String, Object>>() { });
    }

    public Map<String, Object> report(String type, String from, String to, String foodItemId,
                                      String programmeId, String days) {
        Query query = queryFor(type, from, to, foodItemId, programmeId, days);
        return api.get(query.uri(), new ParameterizedTypeReference<Map<String, Object>>() { }, query.vars());
    }

    public ResponseEntity<byte[]> csv(String type, String from, String to, String foodItemId,
                                      String programmeId, String days) {
        Query query = queryFor(type, from, to, foodItemId, programmeId, days).add("format", "csv");
        return api.getBytes(query.uri(), query.vars());
    }

    public Map<String, Object> serviceStatus() {
        return api.get("/api/v1/reports/services/status", new ParameterizedTypeReference<Map<String, Object>>() { });
    }

    private Query queryFor(String type, String from, String to, String foodItemId, String programmeId, String days) {
        return new Query("/api/v1/reports/" + type).add("from", from).add("to", to)
                .add("foodItemId", foodItemId).add("programmeId", programmeId).add("days", days);
    }
}

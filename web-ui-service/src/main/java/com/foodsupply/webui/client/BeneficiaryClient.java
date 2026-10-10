package com.foodsupply.webui.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class BeneficiaryClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP =
            new ParameterizedTypeReference<Map<String, Object>>() { };

    private final BackendClient api;

    public BeneficiaryClient(@Value("${services.beneficiary.url}") String baseUrl) {
        this.api = new BackendClient("beneficiary-service", baseUrl);
    }

    public Map<String, Object> list(String q, String category, String type, String status, int page, int size) {
        Query query = new Query("/api/v1/beneficiaries")
                .add("q", q).add("category", category).add("type", type).add("status", status)
                .add("page", page).add("size", size);
        return api.get(query.uri(), MAP, query.vars());
    }

    public Map<String, Object> get(Long id) {
        return api.get("/api/v1/beneficiaries/{id}", MAP, id);
    }

    public Map<String, Object> create(Map<String, Object> body) {
        return api.post("/api/v1/beneficiaries", body, MAP);
    }

    public Map<String, Object> update(Long id, Map<String, Object> body) {
        return api.put("/api/v1/beneficiaries/{id}", body, MAP, id);
    }

    public void delete(Long id) {
        api.delete("/api/v1/beneficiaries/{id}", id);
    }

    public Map<String, Object> verify(Long id) {
        return api.post("/api/v1/beneficiaries/{id}/verify", null, MAP, id);
    }

    public Map<String, Object> setStatus(Long id, String value) {
        return api.patch("/api/v1/beneficiaries/{id}/status?value={value}", MAP, id, value);
    }
}

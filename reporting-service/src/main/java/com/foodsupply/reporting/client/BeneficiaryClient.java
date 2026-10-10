package com.foodsupply.reporting.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BeneficiaryClient extends BaseClient {

    public BeneficiaryClient(@Value("${services.beneficiary.url}") String baseUrl) {
        super("beneficiary-service", baseUrl);
    }

    public JsonNode stats() {
        return get("/api/v1/beneficiaries/stats");
    }

    public JsonNode list() {
        return get("/api/v1/beneficiaries", params("size", PAGE_SIZE));
    }
}

package com.foodsupply.programme.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.foodsupply.programme.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BeneficiaryClient {

    private static final String SVC = "beneficiary-service";
    private final RestClient client;

    public BeneficiaryClient(@Value("${services.beneficiary.url}") String baseUrl) {
        this.client = ClientSupport.build(baseUrl);
    }

    public JsonNode getById(Long id) {
        return ClientSupport.call(SVC, () -> client.get()
                .uri("/api/v1/beneficiaries/{id}", id).retrieve().body(JsonNode.class));
    }

    /** Returns the paged response ({content: [...]}) of the beneficiary list. */
    public JsonNode search(String q, String type) {
        return ClientSupport.call(SVC, () -> client.get()
                .uri(u -> u.path("/api/v1/beneficiaries").queryParam("q", q).queryParam("type", type)
                        .queryParam("size", 20).build())
                .retrieve().body(JsonNode.class));
    }

    /** Returns null when no requirement is defined for the category. */
    public JsonNode getRequirement(String category) {
        try {
            return ClientSupport.call(SVC, () -> client.get()
                    .uri("/api/v1/nutrition-requirements/category/{c}", category).retrieve().body(JsonNode.class));
        } catch (ResourceNotFoundException e) {
            return null;
        }
    }
}

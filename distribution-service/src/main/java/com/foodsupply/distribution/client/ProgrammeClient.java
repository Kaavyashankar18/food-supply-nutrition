package com.foodsupply.distribution.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

@Component
public class ProgrammeClient {

    private static final String SVC = "programme-service";
    private final RestClient client;

    public ProgrammeClient(@Value("${services.programme.url}") String baseUrl) {
        this.client = ClientSupport.build(baseUrl);
    }

    public JsonNode requirements(Long programmeId, LocalDate from, LocalDate to) {
        return ClientSupport.call(SVC, () -> client.get()
                .uri(u -> u.path("/api/v1/programmes/{id}/requirements").queryParam("from", from.toString())
                        .queryParam("to", to.toString()).build(programmeId))
                .retrieve().body(JsonNode.class));
    }

    /** JSON array of participations (beneficiaryId, beneficiaryName, peopleCovered, status). */
    public JsonNode participants(Long programmeId) {
        return ClientSupport.call(SVC, () -> client.get()
                .uri("/api/v1/programmes/{id}/participants", programmeId).retrieve().body(JsonNode.class));
    }
}

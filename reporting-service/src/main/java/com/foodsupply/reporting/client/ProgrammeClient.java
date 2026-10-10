package com.foodsupply.reporting.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ProgrammeClient extends BaseClient {

    public ProgrammeClient(@Value("${services.programme.url}") String baseUrl) {
        super("programme-service", baseUrl);
    }

    public JsonNode summary() {
        return get("/api/v1/programmes/summary");
    }

    public JsonNode programmes() {
        return get("/api/v1/programmes", params("size", PAGE_SIZE));
    }

    public JsonNode requirements(Long programmeId, LocalDate from, LocalDate to) {
        return get("/api/v1/programmes/" + programmeId + "/requirements",
                params("from", from == null ? null : from.toString(), "to", to == null ? null : to.toString()));
    }
}

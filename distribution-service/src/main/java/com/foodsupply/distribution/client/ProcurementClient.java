package com.foodsupply.distribution.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class ProcurementClient {

    private static final String SVC = "procurement-service";
    private final RestClient client;

    public ProcurementClient(@Value("${services.procurement.url}") String baseUrl) {
        this.client = ClientSupport.build(baseUrl);
    }

    public JsonNode createRequest(Map<String, Object> body) {
        return ClientSupport.call(SVC, () -> client.post().uri("/api/v1/procurement-requests")
                .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class));
    }
}

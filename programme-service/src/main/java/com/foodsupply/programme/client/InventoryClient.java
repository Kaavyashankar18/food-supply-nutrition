package com.foodsupply.programme.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class InventoryClient {

    private static final String SVC = "inventory-service";
    private final RestClient client;

    public InventoryClient(@Value("${services.inventory.url}") String baseUrl) {
        this.client = ClientSupport.build(baseUrl);
    }

    public JsonNode availability(Map<String, Object> body) {
        return ClientSupport.call(SVC, () -> client.post().uri("/api/v1/stock/availability")
                .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class));
    }
}

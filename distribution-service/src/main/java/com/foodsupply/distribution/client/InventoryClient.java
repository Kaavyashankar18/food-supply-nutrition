package com.foodsupply.distribution.client;

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

    /** All-or-nothing; a shortage becomes a ShortageException (HTTP 409 with the shortage list). */
    public JsonNode reserve(Map<String, Object> body) {
        return post("/api/v1/stock/reserve", body);
    }

    public JsonNode dispatch(Map<String, Object> body) {
        return post("/api/v1/stock/dispatch", body);
    }

    public JsonNode release(Map<String, Object> body) {
        return post("/api/v1/stock/release", body);
    }

    private JsonNode post(String path, Map<String, Object> body) {
        return ClientSupport.call(SVC, () -> client.post().uri(path)
                .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class));
    }
}

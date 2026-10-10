package com.foodsupply.programme.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class FoodClient {

    private static final String SVC = "food-nutrition-service";
    private final RestClient client;

    public FoodClient(@Value("${services.food.url}") String baseUrl) {
        this.client = ClientSupport.build(baseUrl);
    }

    /** Plain JSON array of food items (id, name, unit, active, ...). */
    public JsonNode byIds(Collection<Long> ids) {
        String joined = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        return ClientSupport.call(SVC, () -> client.get()
                .uri(u -> u.path("/api/v1/food-items/by-ids").queryParam("ids", joined).build())
                .retrieve().body(JsonNode.class));
    }

    public JsonNode calculate(Map<String, Object> body) {
        return ClientSupport.call(SVC, () -> client.post().uri("/api/v1/meal-calculations")
                .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class));
    }
}

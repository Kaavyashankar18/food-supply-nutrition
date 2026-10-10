package com.foodsupply.reporting.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FoodClient extends BaseClient {

    public FoodClient(@Value("${services.food.url}") String baseUrl) {
        super("food-nutrition-service", baseUrl);
    }

    public JsonNode stats() {
        return get("/api/v1/food-items/stats");
    }
}

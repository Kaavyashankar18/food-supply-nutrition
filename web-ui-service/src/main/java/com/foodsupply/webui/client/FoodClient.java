package com.foodsupply.webui.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class FoodClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP =
            new ParameterizedTypeReference<Map<String, Object>>() { };
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST =
            new ParameterizedTypeReference<List<Map<String, Object>>>() { };

    private final BackendClient api;

    public FoodClient(@Value("${services.food.url}") String baseUrl) {
        this.api = new BackendClient("food-nutrition-service", baseUrl);
    }

    public Map<String, Object> list(String q, String categoryId, String unit, String active, int page, int size) {
        Query query = new Query("/api/v1/food-items")
                .add("q", q).add("categoryId", categoryId).add("unit", unit).add("active", active)
                .add("page", page).add("size", size);
        return api.get(query.uri(), MAP, query.vars());
    }

    public Map<String, Object> get(Long id) {
        return api.get("/api/v1/food-items/{id}", MAP, id);
    }

    public Map<String, Object> create(Map<String, Object> body) {
        return api.post("/api/v1/food-items", body, MAP);
    }

    public Map<String, Object> update(Long id, Map<String, Object> body) {
        return api.put("/api/v1/food-items/{id}", body, MAP, id);
    }

    public Map<String, Object> setActive(Long id, boolean value) {
        return api.patch("/api/v1/food-items/{id}/active?value={value}", MAP, id, value);
    }

    public List<Map<String, Object>> categories() {
        return api.get("/api/v1/food-categories/all", LIST);
    }

    public Map<String, Object> createCategory(String name) {
        return api.post("/api/v1/food-categories", Map.of("name", name), MAP);
    }

    public void deleteCategory(Long id) {
        api.delete("/api/v1/food-categories/{id}", id);
    }
}

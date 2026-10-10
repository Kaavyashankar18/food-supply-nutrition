package com.foodsupply.procurement.client;

import com.foodsupply.procurement.dto.FoodItemInfo;
import com.foodsupply.procurement.util.DownstreamErrors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FoodNutritionClient {

    private static final String SERVICE = "food-nutrition-service";

    private final RestClient restClient;

    public FoodNutritionClient(RestClient.Builder builder, @Value("${services.food.url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(10000);
        this.restClient = builder.baseUrl(baseUrl).requestFactory(factory).build();
    }

    /** GET /api/v1/food-items/by-ids?ids=1,2,3 (plain list). */
    public List<FoodItemInfo> getByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        String joined = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        try {
            List<FoodItemInfo> result = restClient.get()
                    .uri("/api/v1/food-items/by-ids?ids={ids}", joined)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<FoodItemInfo>>() {
                    });
            return result == null ? List.of() : result;
        } catch (RestClientException ex) {
            throw DownstreamErrors.translate(SERVICE, ex);
        }
    }
}

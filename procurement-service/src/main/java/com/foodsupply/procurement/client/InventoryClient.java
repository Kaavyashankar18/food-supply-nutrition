package com.foodsupply.procurement.client;

import com.foodsupply.procurement.dto.BatchInfo;
import com.foodsupply.procurement.dto.ReceiveStockRequest;
import com.foodsupply.procurement.util.DownstreamErrors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class InventoryClient {

    private static final String SERVICE = "inventory-service";

    private final RestClient restClient;

    public InventoryClient(RestClient.Builder builder, @Value("${services.inventory.url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(10000);
        this.restClient = builder.baseUrl(baseUrl).requestFactory(factory).build();
    }

    /** POST /api/v1/stock/receive. A 409 (duplicate batch number) is propagated as BusinessException. */
    public BatchInfo receiveStock(ReceiveStockRequest request) {
        try {
            BatchInfo batch = restClient.post()
                    .uri("/api/v1/stock/receive")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(BatchInfo.class);
            log.info("Inventory received batch {} for food item {}", request.batchNumber(), request.foodItemId());
            return batch;
        } catch (RestClientException ex) {
            throw DownstreamErrors.translate(SERVICE, ex);
        }
    }
}

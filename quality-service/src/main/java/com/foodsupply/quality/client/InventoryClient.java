package com.foodsupply.quality.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodsupply.quality.dto.BatchInfo;
import com.foodsupply.quality.dto.StockInfo;
import com.foodsupply.quality.dto.WriteOffRequest;
import com.foodsupply.quality.entity.QualityStatus;
import com.foodsupply.quality.exception.BusinessException;
import com.foodsupply.quality.exception.DownstreamServiceException;
import com.foodsupply.quality.exception.InvalidRequestException;
import com.foodsupply.quality.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

/**
 * REST client for inventory-service. Connection problems and 5xx answers become a
 * DownstreamServiceException (HTTP 503); 404 / 409 / 400 are translated to the matching
 * local exceptions carrying inventory's message.
 */
@Slf4j
@Component
public class InventoryClient {

    private static final String SERVICE = "inventory-service";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RestClient restClient;

    public InventoryClient(@Value("${services.inventory.url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(10000);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    /** GET /api/v1/batches/{id} */
    public BatchInfo getBatch(Long batchId) {
        return call(() -> restClient.get()
                .uri("/api/v1/batches/{id}", batchId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleError)
                .body(BatchInfo.class));
    }

    /** GET /api/v1/stock/{foodItemId} (used to look up the unit of an item). */
    public StockInfo getStock(Long foodItemId) {
        return call(() -> restClient.get()
                .uri("/api/v1/stock/{foodItemId}", foodItemId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleError)
                .body(StockInfo.class));
    }

    /** PATCH /api/v1/batches/{id}/quality-status?value=...&remarks=... (query parameters, no body). */
    public BatchInfo updateQualityStatus(Long batchId, QualityStatus status, String remarks) {
        boolean hasRemarks = remarks != null && !remarks.isBlank();
        return call(() -> {
            RestClient.RequestBodySpec spec;
            if (hasRemarks) {
                spec = restClient.patch()
                        .uri("/api/v1/batches/{id}/quality-status?value={value}&remarks={remarks}",
                                batchId, status.name(), remarks);
            } else {
                spec = restClient.patch()
                        .uri("/api/v1/batches/{id}/quality-status?value={value}", batchId, status.name());
            }
            return spec.retrieve()
                    .onStatus(HttpStatusCode::isError, this::handleError)
                    .body(BatchInfo.class);
        });
    }

    /** POST /api/v1/batches/{id}/write-off */
    public BatchInfo writeOff(Long batchId, BigDecimal quantity, String reason, Long referenceId) {
        WriteOffRequest body = new WriteOffRequest(quantity, reason, referenceId);
        return call(() -> restClient.post()
                .uri("/api/v1/batches/{id}/write-off", batchId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleError)
                .body(BatchInfo.class));
    }

    private <T> T call(Supplier<T> action) {
        try {
            return action.get();
        } catch (ResourceAccessException ex) {
            log.error("{} is not reachable: {}", SERVICE, ex.getMessage());
            throw new DownstreamServiceException(SERVICE + " is unavailable");
        }
    }

    private void handleError(HttpRequest request, ClientHttpResponse response) throws IOException {
        int code = response.getStatusCode().value();
        String message = extractMessage(response);
        log.warn("{} answered HTTP {} for {} {}: {}", SERVICE, code, request.getMethod(), request.getURI(), message);
        if (code == 404) {
            throw new ResourceNotFoundException(message != null ? message : "Resource not found in " + SERVICE);
        }
        if (code == 409) {
            throw new BusinessException(message != null ? message : SERVICE + " rejected the operation");
        }
        if (code >= 500) {
            throw new DownstreamServiceException(SERVICE + " is unavailable (HTTP " + code + ")");
        }
        throw new InvalidRequestException(message != null ? message : SERVICE + " rejected the request (HTTP " + code + ")");
    }

    private String extractMessage(ClientHttpResponse response) {
        try {
            String raw = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
            if (raw.isBlank()) {
                return null;
            }
            JsonNode node = MAPPER.readTree(raw);
            return node.path("message").asText(null);
        } catch (Exception ex) {
            return null;
        }
    }
}

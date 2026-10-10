package com.foodsupply.webui.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodsupply.webui.exception.ApiException;
import com.foodsupply.webui.exception.DownstreamServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.function.Supplier;

/**
 * Small reusable wrapper around RestClient. Every service-specific client owns one instance.
 * Connection failures and 5xx answers become DownstreamServiceException,
 * 4xx answers become ApiException carrying the backend's message.
 */
@Slf4j
public class BackendClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RestClient client;
    private final String serviceName;

    public BackendClient(String serviceName, String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(15000);
        this.serviceName = serviceName;
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public <T> T get(String uri, ParameterizedTypeReference<T> type, Object... vars) {
        return call(() -> client.get().uri(uri, vars).retrieve().body(type));
    }

    public ResponseEntity<byte[]> getBytes(String uri, Object... vars) {
        return call(() -> client.get().uri(uri, vars).retrieve().toEntity(byte[].class));
    }

    public <T> T post(String uri, Object body, ParameterizedTypeReference<T> type, Object... vars) {
        return call(() -> {
            RestClient.RequestBodySpec spec = client.post().uri(uri, vars).contentType(MediaType.APPLICATION_JSON);
            if (body != null) {
                spec.body(body);
            }
            return spec.retrieve().body(type);
        });
    }

    public <T> T put(String uri, Object body, ParameterizedTypeReference<T> type, Object... vars) {
        return call(() -> client.put().uri(uri, vars).contentType(MediaType.APPLICATION_JSON)
                .body(body).retrieve().body(type));
    }

    public <T> T patch(String uri, ParameterizedTypeReference<T> type, Object... vars) {
        return call(() -> client.patch().uri(uri, vars).retrieve().body(type));
    }

    public void delete(String uri, Object... vars) {
        call(() -> client.delete().uri(uri, vars).retrieve().toBodilessEntity());
    }

    private <T> T call(Supplier<T> action) {
        try {
            return action.get();
        } catch (RestClientResponseException ex) {
            throw translate(ex);
        } catch (RestClientException ex) {
            log.warn("{} call failed: {}", serviceName, ex.getMessage());
            throw new DownstreamServiceException(serviceName + " is unavailable");
        }
    }

    private RuntimeException translate(RestClientResponseException ex) {
        int status = ex.getStatusCode().value();
        if (status >= 500) {
            return new DownstreamServiceException(serviceName + " returned an error (HTTP " + status + ")");
        }
        String message = null;
        try {
            JsonNode node = MAPPER.readTree(ex.getResponseBodyAsString());
            if (node != null && node.hasNonNull("message")) {
                message = node.get("message").asText();
            }
        } catch (Exception ignored) {
            // body was not JSON
        }
        if (message == null || message.isBlank()) {
            message = serviceName + " rejected the request (HTTP " + status + ")";
        }
        return new ApiException(status, message);
    }
}

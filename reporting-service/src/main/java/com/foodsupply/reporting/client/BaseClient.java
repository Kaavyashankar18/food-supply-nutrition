package com.foodsupply.reporting.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.foodsupply.reporting.exception.DownstreamServiceException;
import com.foodsupply.reporting.util.JsonUtil;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;

import java.util.Map;

/** Shared GET logic for the per-service clients: timeouts 3s/10s, any failure becomes a DownstreamServiceException. */
public abstract class BaseClient {

    protected static final int PAGE_SIZE = 1000;

    private final String serviceName;
    private final String baseUrl;
    private final RestClient restClient;

    protected BaseClient(String serviceName, String baseUrl) {
        this.serviceName = serviceName;
        this.baseUrl = baseUrl;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(10000);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public String serviceName() {
        return serviceName;
    }

    public String baseUrl() {
        return baseUrl;
    }

    protected static Map<String, Object> params(Object... keyValues) {
        return JsonUtil.pairs(keyValues);
    }

    protected JsonNode get(String path) {
        return get(path, Map.of());
    }

    protected JsonNode get(String path, Map<String, Object> queryParams) {
        try {
            JsonNode body = restClient.get()
                    .uri(uriBuilder -> {
                        UriBuilder b = uriBuilder.path(path);
                        queryParams.forEach((k, v) -> b.queryParam(k, v));
                        return b.build();
                    })
                    .retrieve()
                    .body(JsonNode.class);
            return body == null ? MissingNode.getInstance() : body;
        } catch (ResourceAccessException e) {
            throw new DownstreamServiceException(serviceName + " is unavailable");
        } catch (RestClientResponseException e) {
            throw new DownstreamServiceException(serviceName + " returned HTTP " + e.getStatusCode().value()
                    + " for " + path);
        } catch (RestClientException e) {
            throw new DownstreamServiceException(serviceName + " returned an unreadable response for " + path);
        }
    }
}

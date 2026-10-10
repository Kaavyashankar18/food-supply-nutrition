package com.foodsupply.distribution.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodsupply.distribution.exception.BusinessException;
import com.foodsupply.distribution.exception.DownstreamServiceException;
import com.foodsupply.distribution.exception.InvalidRequestException;
import com.foodsupply.distribution.exception.ResourceNotFoundException;
import com.foodsupply.distribution.exception.ShortageException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.function.Supplier;

public final class ClientSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ClientSupport() {
    }

    public static RestClient build(String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(10000);
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public static <T> T call(String service, Supplier<T> action) {
        try {
            return action.get();
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            String msg = extractMessage(e.getResponseBodyAsString());
            if (status == 404) {
                throw new ResourceNotFoundException(service + ": " + msg);
            }
            if (status == 409) {
                JsonNode shortages = extractShortages(e.getResponseBodyAsString());
                if (shortages != null) {
                    throw new ShortageException(msg, shortages);
                }
                throw new BusinessException(msg);
            }
            if (status == 400) {
                throw new InvalidRequestException(service + ": " + msg);
            }
            throw new DownstreamServiceException(service + " is unavailable (HTTP " + status + ")");
        } catch (ResourceAccessException e) {
            throw new DownstreamServiceException(service + " is unavailable");
        }
    }

    private static JsonNode extractShortages(String body) {
        try {
            JsonNode n = MAPPER.readTree(body);
            if (n != null && n.has("shortages") && n.get("shortages").isArray()) {
                return n.get("shortages");
            }
        } catch (Exception ignored) {
            // not JSON
        }
        return null;
    }

    private static String extractMessage(String body) {
        try {
            JsonNode n = MAPPER.readTree(body);
            if (n != null && n.hasNonNull("message")) {
                return n.get("message").asText();
            }
        } catch (Exception ignored) {
            // fall through
        }
        return "request failed";
    }
}

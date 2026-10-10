package com.foodsupply.programme.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodsupply.programme.exception.BusinessException;
import com.foodsupply.programme.exception.DownstreamServiceException;
import com.foodsupply.programme.exception.InvalidRequestException;
import com.foodsupply.programme.exception.ResourceNotFoundException;
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

package com.foodsupply.procurement.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodsupply.procurement.exception.BusinessException;
import com.foodsupply.procurement.exception.DownstreamServiceException;
import com.foodsupply.procurement.exception.InvalidRequestException;
import com.foodsupply.procurement.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Translates RestClient failures into this service's exceptions. */
@Slf4j
public final class DownstreamErrors {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private DownstreamErrors() {
    }

    public static RuntimeException translate(String serviceName, RestClientException ex) {
        if (ex instanceof RestClientResponseException responseEx) {
            int code = responseEx.getStatusCode().value();
            String message = extractMessage(responseEx.getResponseBodyAsString(),
                    serviceName + " returned HTTP " + code);
            if (code == 404) {
                return new ResourceNotFoundException(message);
            }
            if (code == 409) {
                return new BusinessException(message);
            }
            if (code == 400) {
                return new InvalidRequestException(message);
            }
            log.error("{} answered HTTP {}: {}", serviceName, code, message);
            if (code >= 500) {
                return new DownstreamServiceException(serviceName + " is unavailable (HTTP " + code + ")");
            }
            return new DownstreamServiceException(serviceName + " rejected the request (HTTP " + code + "): " + message);
        }
        if (ex instanceof ResourceAccessException) {
            log.error("Cannot reach {}: {}", serviceName, ex.getMessage());
            return new DownstreamServiceException(serviceName + " is unavailable");
        }
        log.error("Call to {} failed", serviceName, ex);
        return new DownstreamServiceException(serviceName + " call failed: " + ex.getMessage());
    }

    private static String extractMessage(String body, String fallback) {
        if (body == null || body.isBlank()) {
            return fallback;
        }
        try {
            JsonNode node = MAPPER.readTree(body);
            String message = node.path("message").asText("");
            return message.isBlank() ? fallback : message;
        } catch (Exception e) {
            return fallback;
        }
    }
}

package com.foodsupply.reporting.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.foodsupply.reporting.exception.DownstreamServiceException;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * One instance per request. Runs downstream calls and, when a service fails, records its name and
 * returns an empty (missing) node so the report can still be produced from the other services.
 */
@Slf4j
public class DownstreamTracker {

    private final Set<String> unavailable = new LinkedHashSet<>();

    public JsonNode node(String service, Supplier<JsonNode> call) {
        try {
            JsonNode result = call.get();
            return result == null ? MissingNode.getInstance() : result;
        } catch (DownstreamServiceException e) {
            log.warn("{} could not be reached: {}", service, e.getMessage());
            unavailable.add(service);
            return MissingNode.getInstance();
        }
    }

    public List<String> unavailable() {
        return new ArrayList<>(unavailable);
    }
}

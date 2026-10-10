package com.foodsupply.distribution.exception;

import com.fasterxml.jackson.databind.JsonNode;

/** Inventory could not reserve the stock; carries the shortage list returned by inventory-service. */
public class ShortageException extends BusinessException {

    private final transient JsonNode shortages;

    public ShortageException(String message, JsonNode shortages) {
        super(message);
        this.shortages = shortages;
    }

    public JsonNode getShortages() {
        return shortages;
    }
}

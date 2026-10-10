package com.foodsupply.webui.client;

import java.util.ArrayList;
import java.util.List;

/** Builds a uri template like /path?q={q}&page={page} using only the values that were supplied. */
public class Query {

    private final StringBuilder uri;
    private final List<Object> values = new ArrayList<>();
    private boolean first = true;

    public Query(String path) {
        this.uri = new StringBuilder(path);
    }

    public Query add(String name, Object value) {
        if (value == null || value.toString().isBlank()) {
            return this;
        }
        uri.append(first ? '?' : '&').append(name).append("={").append(name).append('}');
        first = false;
        values.add(value);
        return this;
    }

    public String uri() {
        return uri.toString();
    }

    public Object[] vars() {
        return values.toArray();
    }
}

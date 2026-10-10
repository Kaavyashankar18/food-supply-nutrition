package com.foodsupply.reporting.util;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Null-safe helpers for reading the JSON returned by the other services. */
public final class JsonUtil {

    private JsonUtil() {
    }

    /** Elements of a plain JSON array or of the "content" array of a PageResponse. */
    public static List<JsonNode> items(JsonNode node) {
        List<JsonNode> out = new ArrayList<>();
        if (node == null || node.isMissingNode() || node.isNull()) {
            return out;
        }
        JsonNode array = node.isArray() ? node : node.path("content");
        if (array.isArray()) {
            for (JsonNode element : array) {
                out.add(element);
            }
        }
        return out;
    }

    /** Scalar value of a field (Long, BigDecimal, Boolean or String) or null if absent. */
    public static Object value(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode n = node.path(field);
        if (n.isMissingNode() || n.isNull()) {
            return null;
        }
        if (n.isIntegralNumber()) {
            return n.asLong();
        }
        if (n.isNumber()) {
            return n.decimalValue();
        }
        if (n.isBoolean()) {
            return n.asBoolean();
        }
        if (n.isTextual()) {
            return n.asText();
        }
        return n.toString();
    }

    public static String text(JsonNode node, String field) {
        JsonNode n = node.path(field);
        return n.isMissingNode() || n.isNull() ? "" : n.asText();
    }

    public static BigDecimal decimal(JsonNode node, String field) {
        JsonNode n = node.path(field);
        return n.isNumber() ? n.decimalValue() : BigDecimal.ZERO;
    }

    public static LocalDate date(JsonNode node, String field) {
        String t = text(node, field);
        if (t.length() < 10) {
            return null;
        }
        try {
            return LocalDate.parse(t.substring(0, 10));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** True when the date is inside [from, to]; a missing date or missing bound never excludes. */
    public static boolean inRange(LocalDate d, LocalDate from, LocalDate to) {
        if (d == null) {
            return true;
        }
        if (from != null && d.isBefore(from)) {
            return false;
        }
        return to == null || !d.isAfter(to);
    }

    public static Map<String, Object> row(JsonNode node, List<String> columns) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (String column : columns) {
            row.put(column, value(node, column));
        }
        return row;
    }

    public static Map<String, Long> countBy(List<JsonNode> items, String field) {
        Map<String, Long> out = new LinkedHashMap<>();
        for (JsonNode item : items) {
            String key = text(item, field);
            out.merge(key.isEmpty() ? "UNKNOWN" : key, 1L, Long::sum);
        }
        return out;
    }

    /** Counts per month (yyyy-MM) of a date field, ordered chronologically. */
    public static Map<String, Long> countByMonth(List<JsonNode> items, String dateField) {
        Map<String, Long> out = new TreeMap<>();
        for (JsonNode item : items) {
            String t = text(item, dateField);
            if (t.length() >= 7) {
                out.merge(t.substring(0, 7), 1L, Long::sum);
            }
        }
        return out;
    }

    public static Map<String, BigDecimal> sumBy(List<JsonNode> items, String keyField, String valueField) {
        Map<String, BigDecimal> out = new LinkedHashMap<>();
        for (JsonNode item : items) {
            String key = text(item, keyField);
            out.merge(key.isEmpty() ? "UNKNOWN" : key, decimal(item, valueField), BigDecimal::add);
        }
        return out;
    }

    /** Converts a flat JSON object such as {"Grains":5,"Pulses":4} into a map. */
    public static Map<String, Object> objectToMap(JsonNode node) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (node == null || !node.isObject()) {
            return out;
        }
        for (Iterator<String> it = node.fieldNames(); it.hasNext(); ) {
            String name = it.next();
            out.put(name, value(node, name));
        }
        return out;
    }

    /** Builds an ordered map from key/value pairs, skipping pairs whose value is null. */
    public static Map<String, Object> pairs(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            if (keyValues[i + 1] != null) {
                map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
            }
        }
        return map;
    }
}

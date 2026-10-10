package com.foodsupply.webui.util;

import org.springframework.stereotype.Component;

import java.util.Set;

/** Small helpers used from templates as ${@ui.xxx(...)}. */
@Component("ui")
public class UiHelper {

    private static final Set<String> GOOD = Set.of("ACTIVE", "APPROVED", "PASS", "DELIVERED", "RECEIVED", "COMPLETED",
            "ENROLLED", "CLOSED", "ELIGIBLE");
    private static final Set<String> WARN = Set.of("DRAFT", "PLANNED", "CREATED", "OPEN", "SUBMITTED", "IN_PROGRESS",
            "SENT", "CONDITIONAL", "PARTIAL", "PARTIALLY_RECEIVED", "DISPATCHED", "RESERVED", "ORDERED", "RECEIPT");
    private static final Set<String> BAD = Set.of("REJECTED", "FAIL", "FAILED", "CANCELLED", "QUARANTINED",
            "INACTIVE", "WITHDRAWN", "EXPIRED", "WASTAGE");

    public String pretty(String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        String spaced = key.replace('_', ' ').replaceAll("([a-z0-9])([A-Z])", "$1 $2").trim();
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1).toLowerCase();
    }

    /** Bootstrap colour name for a known status value, or an empty string for ordinary text. */
    public String badge(Object value) {
        if (value == null) {
            return "";
        }
        String v = value.toString().toUpperCase();
        if (GOOD.contains(v)) {
            return "success";
        }
        if (WARN.contains(v)) {
            return "warning";
        }
        if (BAD.contains(v)) {
            return "danger";
        }
        return "";
    }

    public String cell(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Boolean b) {
            return b ? "Yes" : "No";
        }
        return value.toString();
    }
}

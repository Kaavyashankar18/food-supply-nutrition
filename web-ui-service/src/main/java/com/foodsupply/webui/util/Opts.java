package com.foodsupply.webui.util;

import com.foodsupply.webui.dto.Option;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Option lists for select boxes plus a few small conversion helpers. */
public final class Opts {

    private Opts() {
    }

    /** Fixed values; blankLabel (if not null) adds an empty first option. */
    public static List<Option> of(String blankLabel, String... values) {
        List<Option> result = new ArrayList<>();
        if (blankLabel != null) {
            result.add(new Option("", blankLabel));
        }
        for (String v : values) {
            result.add(new Option(v, v));
        }
        return result;
    }

    /** One option per row: value from valueKey, label = the non-null labelKeys joined with " - ". */
    public static List<Option> from(List<Map<String, Object>> rows, String blankLabel, String valueKey,
                                    String... labelKeys) {
        List<Option> result = new ArrayList<>();
        if (blankLabel != null) {
            result.add(new Option("", blankLabel));
        }
        for (Map<String, Object> row : rows) {
            StringBuilder label = new StringBuilder();
            for (String key : labelKeys) {
                Object v = row.get(key);
                if (v != null) {
                    if (label.length() > 0) {
                        label.append(" - ");
                    }
                    label.append(v);
                }
            }
            result.add(new Option(String.valueOf(row.get(valueKey)), label.toString()));
        }
        return result;
    }

    public static String str(Object o) {
        return o == null ? "" : o.toString();
    }

    public static Long toLong(String field, String value) {
        try {
            return Long.valueOf(value.trim());
        } catch (Exception ex) {
            throw new com.foodsupply.webui.exception.ApiException(400, field + ": please enter a whole number");
        }
    }

    public static BigDecimal toDecimal(String field, String value) {
        try {
            return new BigDecimal(value.trim());
        } catch (Exception ex) {
            throw new com.foodsupply.webui.exception.ApiException(400, field + ": please enter a number");
        }
    }

    public static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}

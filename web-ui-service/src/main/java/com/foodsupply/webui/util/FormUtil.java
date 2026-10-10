package com.foodsupply.webui.util;

import com.foodsupply.webui.exception.ApiException;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class FormUtil {

    private FormUtil() {
    }

    /** Turns submitted form values into a JSON body: blanks become null, number fields get real numbers. */
    public static Map<String, Object> payload(Map<String, String> params, Set<String> longFields,
                                              Set<String> decimalFields) {
        Map<String, Object> body = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : params.entrySet()) {
            String key = e.getKey();
            if (key.equals("id") || key.startsWith("_")) {
                continue;
            }
            String value = e.getValue() == null ? "" : e.getValue().trim();
            if (value.isEmpty()) {
                body.put(key, null);
            } else if (longFields.contains(key)) {
                body.put(key, parseLong(key, value));
            } else if (decimalFields.contains(key)) {
                body.put(key, parseDecimal(key, value));
            } else {
                body.put(key, value);
            }
        }
        return body;
    }

    private static Long parseLong(String key, String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            throw new ApiException(400, key + ": please enter a whole number");
        }
    }

    private static BigDecimal parseDecimal(String key, String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            throw new ApiException(400, key + ": please enter a number");
        }
    }

    /** List url with the filters that are set, e.g. listUrl("/food", "q", "rice", "unit", "") -> /food?q=rice */
    public static String listUrl(String path, String... namesAndValues) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(path);
        for (int i = 0; i + 1 < namesAndValues.length; i += 2) {
            String value = namesAndValues[i + 1];
            if (value != null && !value.isBlank()) {
                builder.queryParam(namesAndValues[i], value);
            }
        }
        return builder.build().encode().toUriString();
    }
}

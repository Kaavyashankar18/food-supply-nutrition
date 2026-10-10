package com.foodsupply.reporting.util;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class CsvUtil {

    private static final String BOM = "﻿";

    private CsvUtil() {
    }

    public static String toCsv(List<String> columns, List<Map<String, Object>> rows) {
        StringBuilder sb = new StringBuilder(BOM);
        sb.append(columns.stream().map(CsvUtil::escape).collect(Collectors.joining(","))).append("\r\n");
        for (Map<String, Object> row : rows) {
            sb.append(columns.stream().map(c -> escape(row.get(c))).collect(Collectors.joining(",")))
                    .append("\r\n");
        }
        return sb.toString();
    }

    static String escape(Object value) {
        if (value == null) {
            return "";
        }
        String s = String.valueOf(value);
        // Neutralise spreadsheet formulas in text values (numbers such as -5 stay untouched).
        if (!(value instanceof Number) && !s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) {
            s = "'" + s;
        }
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}

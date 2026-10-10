package com.foodsupply.reporting.util;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvUtilTest {

    @Test
    void writesHeaderAndEscapesSpecialCharacters() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("name", "Rice, Raw \"Premium\"");
        row.put("qty", 12);

        String csv = CsvUtil.toCsv(List.of("name", "qty"), List.of(row));

        assertTrue(csv.startsWith("﻿name,qty\r\n"));
        assertTrue(csv.contains("\"Rice, Raw \"\"Premium\"\"\",12"));
    }

    @Test
    void nullBecomesEmptyAndFormulaTextIsNeutralised() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("a", null);
        row.put("b", "=SUM(A1)");
        row.put("c", -5);

        String csv = CsvUtil.toCsv(List.of("a", "b", "c"), List.of(row));

        assertTrue(csv.endsWith(",'=SUM(A1),-5\r\n"));
        assertEquals("", CsvUtil.escape(null));
    }
}

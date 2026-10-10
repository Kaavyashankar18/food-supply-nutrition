package com.foodsupply.reporting.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonUtilTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void itemsReadsPageContentAndPlainArrays() throws Exception {
        JsonNode page = mapper.readTree("{\"content\":[{\"id\":1},{\"id\":2}],\"page\":0}");
        JsonNode array = mapper.readTree("[{\"id\":1}]");

        assertEquals(2, JsonUtil.items(page).size());
        assertEquals(1, JsonUtil.items(array).size());
        assertTrue(JsonUtil.items(null).isEmpty());
    }

    @Test
    void valueReturnsTypedScalarsOrNull() throws Exception {
        JsonNode n = mapper.readTree("{\"a\":5,\"b\":2.5,\"c\":true,\"d\":\"x\",\"e\":null}");

        assertEquals(5L, JsonUtil.value(n, "a"));
        assertEquals(new BigDecimal("2.5"), JsonUtil.value(n, "b"));
        assertEquals(true, JsonUtil.value(n, "c"));
        assertEquals("x", JsonUtil.value(n, "d"));
        assertNull(JsonUtil.value(n, "e"));
        assertNull(JsonUtil.value(n, "missing"));
    }

    @Test
    void inRangeKeepsMissingDatesAndChecksBounds() {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 31);

        assertTrue(JsonUtil.inRange(null, from, to));
        assertTrue(JsonUtil.inRange(LocalDate.of(2026, 10, 10), from, to));
        assertFalse(JsonUtil.inRange(LocalDate.of(2026, 9, 30), from, to));
        assertFalse(JsonUtil.inRange(LocalDate.of(2026, 11, 1), from, to));
        assertTrue(JsonUtil.inRange(LocalDate.of(2030, 1, 1), from, null));
    }

    @Test
    void countAndSumGroupByField() throws Exception {
        List<JsonNode> items = JsonUtil.items(mapper.readTree(
                "[{\"reason\":\"SPOILED\",\"unit\":\"KG\",\"quantity\":5},"
                        + "{\"reason\":\"SPOILED\",\"unit\":\"KG\",\"quantity\":2.5},"
                        + "{\"reason\":\"EXPIRED\",\"unit\":\"LITRE\",\"quantity\":10}]"));

        Map<String, Long> counts = JsonUtil.countBy(items, "reason");
        Map<String, BigDecimal> sums = JsonUtil.sumBy(items, "unit", "quantity");

        assertEquals(2L, counts.get("SPOILED"));
        assertEquals(1L, counts.get("EXPIRED"));
        assertEquals(0, new BigDecimal("7.5").compareTo(sums.get("KG")));
        assertEquals(0, new BigDecimal("10").compareTo(sums.get("LITRE")));
    }
}

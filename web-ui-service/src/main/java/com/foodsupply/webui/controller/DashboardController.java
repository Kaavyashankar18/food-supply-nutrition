package com.foodsupply.webui.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodsupply.webui.client.ReportingClient;
import com.foodsupply.webui.dto.CardView;
import com.foodsupply.webui.dto.ChartView;
import com.foodsupply.webui.exception.ApiException;
import com.foodsupply.webui.exception.DownstreamServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Dashboard page. All figures come from reporting-service; the card and chart keys are
 * rendered generically so the page follows whatever the reporting dashboard returns.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final ReportingClient reporting;
    private final ObjectMapper mapper;

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        Map<String, Object> data = Collections.emptyMap();
        String problem = null;
        try {
            Map<String, Object> response = reporting.dashboard();
            if (response != null) {
                data = response;
            }
        } catch (DownstreamServiceException | ApiException ex) {
            problem = ex.getMessage();
        }

        List<CardView> cards = new ArrayList<>();
        Object cardsObj = data.get("cards");
        if (cardsObj instanceof Map<?, ?> cardMap) {
            for (Map.Entry<?, ?> entry : cardMap.entrySet()) {
                String key = String.valueOf(entry.getKey());
                cards.add(new CardView(prettify(key), format(entry.getValue()), icon(key), tone(key)));
            }
        }

        List<ChartView> charts = new ArrayList<>();
        Object chartsObj = data.get("charts");
        if (chartsObj instanceof Map<?, ?> chartMap) {
            int index = 0;
            for (Map.Entry<?, ?> entry : chartMap.entrySet()) {
                if (!(entry.getValue() instanceof Map<?, ?> series) || series.isEmpty()) {
                    continue;
                }
                List<String> labels = new ArrayList<>();
                List<Double> values = new ArrayList<>();
                for (Map.Entry<?, ?> point : series.entrySet()) {
                    labels.add(String.valueOf(point.getKey()));
                    values.add(point.getValue() instanceof Number number ? number.doubleValue() : 0.0);
                }
                String key = String.valueOf(entry.getKey());
                charts.add(new ChartView("chart" + index++, prettify(key), chartType(key), labels, values));
            }
        }

        List<String> unavailable = new ArrayList<>();
        if (data.get("unavailable") instanceof List<?> list) {
            for (Object item : list) {
                unavailable.add(String.valueOf(item));
            }
        }

        String chartsJson = "[]";
        try {
            chartsJson = mapper.writeValueAsString(charts);
        } catch (JsonProcessingException ex) {
            log.warn("Could not serialise charts: {}", ex.getMessage());
        }

        model.addAttribute("cards", cards);
        model.addAttribute("charts", charts);
        model.addAttribute("chartsJson", chartsJson);
        model.addAttribute("unavailable", unavailable);
        model.addAttribute("problem", problem);
        return "dashboard/index";
    }

    private static String prettify(String key) {
        String spaced = key.replace('_', ' ').replaceAll("([a-z0-9])([A-Z])", "$1 $2").trim();
        if (spaced.isEmpty()) {
            return key;
        }
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    private static String format(Object value) {
        if (value == null) {
            return "n/a";
        }
        if (value instanceof Double || value instanceof Float) {
            double d = ((Number) value).doubleValue();
            return d == Math.rint(d) ? String.format(Locale.US, "%,.0f", d) : String.format(Locale.US, "%,.1f", d);
        }
        if (value instanceof Number number) {
            return String.format(Locale.US, "%,d", number.longValue());
        }
        return String.valueOf(value);
    }

    private static String icon(String key) {
        String s = key.toLowerCase(Locale.ROOT);
        if (s.contains("beneficiar") || s.contains("people")) {
            return "bi-people-fill";
        }
        if (s.contains("stock") || s.contains("expir") || s.contains("quarantin")) {
            return "bi-box-seam";
        }
        if (s.contains("procure") || s.contains("purchase")) {
            return "bi-cart-check";
        }
        if (s.contains("distribut")) {
            return "bi-truck";
        }
        if (s.contains("programme")) {
            return "bi-journal-check";
        }
        if (s.contains("wastage")) {
            return "bi-trash3";
        }
        if (s.contains("inspection") || s.contains("action") || s.contains("quality")) {
            return "bi-shield-check";
        }
        return "bi-bar-chart-line";
    }

    private static String tone(String key) {
        String s = key.toLowerCase(Locale.ROOT);
        if (s.contains("expired") || s.contains("failed") || s.contains("outofstock") || s.contains("quarantin")) {
            return "danger";
        }
        if (s.contains("low") || s.contains("expiring") || s.contains("pending") || s.contains("open")
                || s.contains("wastage")) {
            return "warning";
        }
        if (s.contains("active") || s.contains("completed") || s.contains("eligible")) {
            return "success";
        }
        return "primary";
    }

    private static String chartType(String key) {
        String s = key.toLowerCase(Locale.ROOT);
        return (s.contains("month") || s.contains("alert")) ? "bar" : "doughnut";
    }
}

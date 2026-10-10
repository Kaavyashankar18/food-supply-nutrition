package com.foodsupply.webui.controller;

import com.foodsupply.webui.client.ReportingClient;
import com.foodsupply.webui.dto.Section;
import com.foodsupply.webui.util.FormUtil;
import com.foodsupply.webui.util.Opts;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private static final Map<String, String> TYPES = new LinkedHashMap<>();

    static {
        TYPES.put("beneficiary", "Beneficiary coverage by category");
        TYPES.put("inventory", "Stock levels and alerts");
        TYPES.put("procurement", "Procurement requests and orders");
        TYPES.put("distribution", "Distribution plans and delivery");
        TYPES.put("programme", "Nutrition programmes");
        TYPES.put("shortage", "Shortage for a programme (needed vs available)");
        TYPES.put("expiry", "Stock nearing expiry");
        TYPES.put("quality-wastage", "Quality inspections and wastage");
    }

    private final ReportingClient reporting;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("title", "Reports");
        model.addAttribute("active", "reports");
        model.addAttribute("types", TYPES);
        return "reports/index";
    }

    @GetMapping("/{type}")
    public String report(@PathVariable String type, @RequestParam(required = false) String from,
                         @RequestParam(required = false) String to, @RequestParam(required = false) String foodItemId,
                         @RequestParam(required = false) String programmeId, @RequestParam(required = false) String days,
                         Model model) {
        if (!TYPES.containsKey(type)) {
            return "error/404";
        }
        Map<String, Object> data = reporting.report(type, from, to, foodItemId, programmeId, days);
        List<Section> sections = new ArrayList<>();
        List<String> columns = new ArrayList<>();
        if (data.get("columns") instanceof List<?> cols) {
            for (Object c : cols) {
                columns.add(columnName(c));
            }
        }
        List<List<String>> rows = new ArrayList<>();
        if (data.get("rows") instanceof List<?> list) {
            for (Object r : list) {
                List<String> cells = new ArrayList<>();
                if (r instanceof List<?> l) {
                    for (Object cell : l) {
                        cells.add(Opts.str(cell));
                    }
                } else if (r instanceof Map<?, ?> m) {
                    for (Object c : (data.get("columns") instanceof List<?> cl ? cl : new ArrayList<>())) {
                        cells.add(Opts.str(m.get(columnKey(c))));
                    }
                }
                rows.add(cells);
            }
        }
        sections.add(new Section("Rows", columns, rows, null));
        if (data.get("totals") instanceof Map<?, ?> totals && !totals.isEmpty()) {
            List<List<String>> pairs = new ArrayList<>();
            for (Map.Entry<?, ?> e : totals.entrySet()) {
                pairs.add(List.of(String.valueOf(e.getKey()), Opts.str(e.getValue())));
            }
            sections.add(new Section("Totals", null, null, pairs));
        }
        List<String> unavailable = new ArrayList<>();
        if (data.get("unavailable") instanceof List<?> u) {
            for (Object o : u) {
                unavailable.add(String.valueOf(o));
            }
        }
        model.addAttribute("title", TYPES.get(type));
        model.addAttribute("active", "reports");
        model.addAttribute("type", type);
        model.addAttribute("sections", sections);
        model.addAttribute("unavailable", unavailable);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("foodItemId", foodItemId);
        model.addAttribute("programmeId", programmeId);
        model.addAttribute("days", days);
        model.addAttribute("csvUrl", FormUtil.listUrl("/reports/" + type + "/csv", "from", from, "to", to,
                "foodItemId", foodItemId, "programmeId", programmeId, "days", days));
        return "reports/report";
    }

    @GetMapping("/{type}/csv")
    public ResponseEntity<byte[]> csv(@PathVariable String type, @RequestParam(required = false) String from,
                                      @RequestParam(required = false) String to,
                                      @RequestParam(required = false) String foodItemId,
                                      @RequestParam(required = false) String programmeId,
                                      @RequestParam(required = false) String days) {
        if (!TYPES.containsKey(type)) {
            return ResponseEntity.notFound().build();
        }
        ResponseEntity<byte[]> upstream = reporting.csv(type, from, to, foodItemId, programmeId, days);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + type + "-report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(upstream.getBody());
    }

    private static String columnName(Object c) {
        if (c instanceof Map<?, ?> m) {
            for (String k : new String[]{"label", "title", "name", "key"}) {
                if (m.get(k) != null) {
                    return m.get(k).toString();
                }
            }
        }
        return String.valueOf(c);
    }

    private static String columnKey(Object c) {
        if (c instanceof Map<?, ?> m) {
            for (String k : new String[]{"key", "name", "field", "label"}) {
                if (m.get(k) != null) {
                    return m.get(k).toString();
                }
            }
        }
        return String.valueOf(c);
    }
}

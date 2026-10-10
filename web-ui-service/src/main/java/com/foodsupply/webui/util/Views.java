package com.foodsupply.webui.util;

import com.foodsupply.webui.dto.Action;
import com.foodsupply.webui.dto.CardView;
import com.foodsupply.webui.dto.Field;
import com.foodsupply.webui.dto.Section;
import com.foodsupply.webui.dto.Tab;
import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Builds the model for the generic list / detail / form templates. */
public final class Views {

    private Views() {
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> rows(Map<String, Object> page) {
        if (page == null || !(page.get("content") instanceof List<?>)) {
            return new ArrayList<>();
        }
        return (List<Map<String, Object>>) page.get("content");
    }

    /** Preferred columns if at least two exist in the data, otherwise every simple column. */
    public static List<String> columns(List<Map<String, Object>> rows, String... preferred) {
        List<String> result = new ArrayList<>();
        if (rows.isEmpty()) {
            for (String p : preferred) {
                result.add(p);
            }
            return result;
        }
        Map<String, Object> first = rows.get(0);
        for (String p : preferred) {
            if (first.containsKey(p)) {
                result.add(p);
            }
        }
        if (result.size() >= 2) {
            return result;
        }
        result.clear();
        for (Map.Entry<String, Object> e : first.entrySet()) {
            Object v = e.getValue();
            if (!(v instanceof Map<?, ?>) && !(v instanceof List<?>)) {
                result.add(e.getKey());
            }
        }
        return result;
    }

    public static void list(Model model, String title, String active, List<Tab> tabs, List<Field> filters,
                            String filterUrl, Map<String, Object> page, List<Map<String, Object>> rows,
                            List<String> columns, String linkBase, String linkKey, String addUrl,
                            String addLabel, String base, List<CardView> cards) {
        model.addAttribute("title", title);
        model.addAttribute("active", active);
        model.addAttribute("tabs", tabs);
        model.addAttribute("filters", filters);
        model.addAttribute("filterUrl", filterUrl);
        model.addAttribute("page", page);
        model.addAttribute("rows", rows);
        model.addAttribute("columns", columns);
        model.addAttribute("linkBase", linkBase);
        model.addAttribute("linkKey", linkKey);
        model.addAttribute("addUrl", addUrl);
        model.addAttribute("addLabel", addLabel);
        model.addAttribute("base", base);
        model.addAttribute("cards", cards == null ? new ArrayList<CardView>() : cards);
    }

    public static void detail(Model model, String title, String active, String backUrl, String backLabel,
                              List<Tab> tabs, List<Section> sections, List<Action> actions) {
        model.addAttribute("title", title);
        model.addAttribute("active", active);
        model.addAttribute("backUrl", backUrl);
        model.addAttribute("backLabel", backLabel);
        model.addAttribute("tabs", tabs == null ? new ArrayList<Tab>() : tabs);
        model.addAttribute("sections", sections);
        model.addAttribute("actions", actions == null ? new ArrayList<Action>() : actions);
    }

    public static void form(Model model, String title, String active, String backUrl, String action,
                            List<Field> fields, String error, String submitLabel) {
        model.addAttribute("title", title);
        model.addAttribute("active", active);
        model.addAttribute("backUrl", backUrl);
        model.addAttribute("formAction", action);
        model.addAttribute("fields", fields);
        model.addAttribute("formError", error);
        model.addAttribute("submitLabel", submitLabel);
    }

    /** Cards for the given summary keys that exist in the map. */
    public static List<CardView> cards(Map<String, Object> summary, String... keys) {
        List<CardView> cards = new ArrayList<>();
        if (summary == null) {
            return cards;
        }
        UiHelper ui = new UiHelper();
        for (String key : keys) {
            if (summary.containsKey(key)) {
                Object v = summary.get(key);
                cards.add(new CardView(ui.pretty(key), v == null ? "n/a" : v.toString(), "", "primary"));
            }
        }
        return cards;
    }

    /** Turns any JSON object into tables and key/value blocks. */
    public static List<Section> sections(Map<String, Object> json) {
        List<Section> result = new ArrayList<>();
        if (json == null) {
            return result;
        }
        List<List<String>> scalars = new ArrayList<>();
        List<Section> later = new ArrayList<>();
        UiHelper ui = new UiHelper();
        for (Map.Entry<String, Object> e : json.entrySet()) {
            Object v = e.getValue();
            String title = ui.pretty(e.getKey());
            if (v instanceof Map<?, ?> m) {
                List<List<String>> pairs = new ArrayList<>();
                for (Map.Entry<?, ?> inner : m.entrySet()) {
                    pairs.add(List.of(ui.pretty(String.valueOf(inner.getKey())), ui.cell(inner.getValue())));
                }
                later.add(new Section(title, null, null, pairs));
            } else if (v instanceof List<?> list) {
                later.add(listSection(title, list, ui));
            } else {
                scalars.add(List.of(title, ui.cell(v)));
            }
        }
        if (!scalars.isEmpty()) {
            result.add(new Section("Details", null, null, scalars));
        }
        result.addAll(later);
        return result;
    }

    private static Section listSection(String title, List<?> list, UiHelper ui) {
        Set<String> keys = new LinkedHashSet<>();
        for (Object o : list) {
            if (o instanceof Map<?, ?> m) {
                for (Object k : m.keySet()) {
                    keys.add(String.valueOf(k));
                }
            }
        }
        if (keys.isEmpty()) {
            List<List<String>> rows = new ArrayList<>();
            for (Object o : list) {
                rows.add(List.of(ui.cell(o)));
            }
            return new Section(title, List.of("Value"), rows, null);
        }
        List<List<String>> rows = new ArrayList<>();
        for (Object o : list) {
            List<String> row = new ArrayList<>();
            if (o instanceof Map<?, ?> m) {
                for (String k : keys) {
                    row.add(ui.cell(m.get(k)));
                }
            }
            rows.add(row);
        }
        return new Section(title, new ArrayList<>(keys), rows, null);
    }
}

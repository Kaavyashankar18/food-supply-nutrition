package com.foodsupply.webui.dto;

import java.util.List;

/** One input of a generic form or filter bar. type: text, number, date, select, textarea, hidden. */
public record Field(String type, String name, String label, String value, boolean required,
                    String hint, List<Option> options) {

    public static Field text(String name, String label, String value) {
        return new Field("text", name, label, value, false, null, List.of());
    }

    public static Field number(String name, String label, String value) {
        return new Field("number", name, label, value, false, null, List.of());
    }

    public static Field date(String name, String label, String value) {
        return new Field("date", name, label, value, false, null, List.of());
    }

    public static Field area(String name, String label, String value) {
        return new Field("textarea", name, label, value, false, null, List.of());
    }

    public static Field hidden(String name, String value) {
        return new Field("hidden", name, name, value, false, null, List.of());
    }

    public static Field select(String name, String label, String value, List<Option> options) {
        return new Field("select", name, label, value, false, null, options);
    }

    public Field req() {
        return new Field(type, name, label, value, true, hint, options);
    }

    public Field hint(String text) {
        return new Field(type, name, label, value, required, text, options);
    }
}

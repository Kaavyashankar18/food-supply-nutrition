package com.foodsupply.webui.dto;

/** A button on a detail page: either a POST (post=true) or a plain link. */
public record Action(String label, String url, boolean post, String style, String confirm) {

    public static Action post(String label, String url, String style, String confirm) {
        return new Action(label, url, true, style, confirm);
    }

    public static Action link(String label, String url, String style) {
        return new Action(label, url, false, style, null);
    }
}

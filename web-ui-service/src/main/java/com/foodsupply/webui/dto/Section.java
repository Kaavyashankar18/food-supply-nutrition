package com.foodsupply.webui.dto;

import java.util.List;

/** Either a table (columns + rows) or a list of label/value pairs. */
public record Section(String title, List<String> columns, List<List<String>> rows, List<List<String>> pairs) {
}

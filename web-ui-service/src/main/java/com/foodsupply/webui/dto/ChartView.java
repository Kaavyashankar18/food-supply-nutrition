package com.foodsupply.webui.dto;

import java.util.List;

public record ChartView(String id, String title, String type, List<String> labels, List<Double> values) {
}

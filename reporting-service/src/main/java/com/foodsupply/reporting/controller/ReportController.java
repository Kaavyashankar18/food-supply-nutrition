package com.foodsupply.reporting.controller;

import com.foodsupply.reporting.dto.DashboardResponse;
import com.foodsupply.reporting.dto.ReportResponse;
import com.foodsupply.reporting.dto.ServiceStatusResponse;
import com.foodsupply.reporting.exception.InvalidRequestException;
import com.foodsupply.reporting.service.ReportService;
import com.foodsupply.reporting.util.CsvUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Reports")
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private static final List<String> DASHBOARD_COLUMNS = List.of("metric", "value");

    private final ReportService reportService;

    @GetMapping("/dashboard")
    public ResponseEntity<Object> dashboard(@RequestParam(name = "format", required = false) String format) {
        DashboardResponse dashboard = reportService.dashboard();
        if (!isCsv(format)) {
            return ResponseEntity.<Object>ok(dashboard);
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        dashboard.cards().forEach((metric, value) -> rows.add(metricRow(metric, value)));
        dashboard.charts().forEach((chart, values) ->
                values.forEach((label, value) -> rows.add(metricRow("chart." + chart + "." + label, value))));
        return csv(CsvUtil.toCsv(DASHBOARD_COLUMNS, rows), "dashboard", dashboard.unavailable());
    }

    @GetMapping("/services/status")
    public List<ServiceStatusResponse> serviceStatus() {
        return reportService.serviceStatus();
    }

    @GetMapping("/beneficiary")
    public ResponseEntity<Object> beneficiary(
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "format", required = false) String format) {
        checkRange(from, to);
        return respond(reportService.beneficiaryReport(from, to), format);
    }

    @GetMapping("/inventory")
    public ResponseEntity<Object> inventory(
            @RequestParam(name = "foodItemId", required = false) Long foodItemId,
            @RequestParam(name = "format", required = false) String format) {
        return respond(reportService.inventoryReport(foodItemId), format);
    }

    @GetMapping("/procurement")
    public ResponseEntity<Object> procurement(
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "foodItemId", required = false) Long foodItemId,
            @RequestParam(name = "format", required = false) String format) {
        checkRange(from, to);
        return respond(reportService.procurementReport(from, to, foodItemId), format);
    }

    @GetMapping("/distribution")
    public ResponseEntity<Object> distribution(
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "programmeId", required = false) Long programmeId,
            @RequestParam(name = "format", required = false) String format) {
        checkRange(from, to);
        return respond(reportService.distributionReport(from, to, programmeId), format);
    }

    @GetMapping("/programme")
    public ResponseEntity<Object> programme(
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "programmeId", required = false) Long programmeId,
            @RequestParam(name = "format", required = false) String format) {
        checkRange(from, to);
        return respond(reportService.programmeReport(from, to, programmeId), format);
    }

    @GetMapping("/shortage")
    public ResponseEntity<Object> shortage(
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "programmeId", required = false) Long programmeId,
            @RequestParam(name = "foodItemId", required = false) Long foodItemId,
            @RequestParam(name = "format", required = false) String format) {
        checkRange(from, to);
        return respond(reportService.shortageReport(from, to, programmeId, foodItemId), format);
    }

    @GetMapping("/expiry")
    public ResponseEntity<Object> expiry(
            @RequestParam(name = "foodItemId", required = false) Long foodItemId,
            @RequestParam(name = "days", required = false) Integer days,
            @RequestParam(name = "format", required = false) String format) {
        return respond(reportService.expiryReport(foodItemId, days), format);
    }

    @GetMapping("/quality-wastage")
    public ResponseEntity<Object> qualityWastage(
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "foodItemId", required = false) Long foodItemId,
            @RequestParam(name = "format", required = false) String format) {
        checkRange(from, to);
        return respond(reportService.qualityWastageReport(from, to, foodItemId), format);
    }

    // ------------------------------------------------------------------ helpers

    private ResponseEntity<Object> respond(ReportResponse report, String format) {
        if (!isCsv(format)) {
            return ResponseEntity.<Object>ok(report);
        }
        return csv(CsvUtil.toCsv(report.columns(), report.rows()), report.report() + "-report", report.unavailable());
    }

    private ResponseEntity<Object> csv(String content, String name, List<String> unavailable) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + name + "-" + LocalDate.now() + ".csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        if (!unavailable.isEmpty()) {
            builder.header("X-Unavailable-Services", String.join(",", unavailable));
        }
        return builder.<Object>body(content);
    }

    private static boolean isCsv(String format) {
        if (format == null || format.isBlank() || "json".equalsIgnoreCase(format)) {
            return false;
        }
        if ("csv".equalsIgnoreCase(format)) {
            return true;
        }
        throw new InvalidRequestException("format must be 'json' or 'csv'");
    }

    private static void checkRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidRequestException("'from' must not be after 'to'");
        }
    }

    private static Map<String, Object> metricRow(String metric, Object value) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("metric", metric);
        row.put("value", value);
        return row;
    }
}

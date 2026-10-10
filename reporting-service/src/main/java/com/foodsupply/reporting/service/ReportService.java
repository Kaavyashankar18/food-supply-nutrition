package com.foodsupply.reporting.service;

import com.foodsupply.reporting.dto.DashboardResponse;
import com.foodsupply.reporting.dto.ReportResponse;
import com.foodsupply.reporting.dto.ServiceStatusResponse;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {

    DashboardResponse dashboard();

    ReportResponse beneficiaryReport(LocalDate from, LocalDate to);

    ReportResponse inventoryReport(Long foodItemId);

    ReportResponse procurementReport(LocalDate from, LocalDate to, Long foodItemId);

    ReportResponse distributionReport(LocalDate from, LocalDate to, Long programmeId);

    ReportResponse programmeReport(LocalDate from, LocalDate to, Long programmeId);

    ReportResponse shortageReport(LocalDate from, LocalDate to, Long programmeId, Long foodItemId);

    ReportResponse expiryReport(Long foodItemId, Integer days);

    ReportResponse qualityWastageReport(LocalDate from, LocalDate to, Long foodItemId);

    List<ServiceStatusResponse> serviceStatus();
}

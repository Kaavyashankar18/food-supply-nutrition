package com.foodsupply.procurement.controller;

import com.foodsupply.procurement.dto.ProcurementSummary;
import com.foodsupply.procurement.service.ProcurementSummaryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Procurement Summary")
@RestController
@RequestMapping("/api/v1/procurement")
@RequiredArgsConstructor
public class ProcurementSummaryController {

    private final ProcurementSummaryService summaryService;

    @GetMapping("/summary")
    public ProcurementSummary summary() {
        return summaryService.summary();
    }
}

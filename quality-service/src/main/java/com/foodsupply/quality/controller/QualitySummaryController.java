package com.foodsupply.quality.controller;

import com.foodsupply.quality.dto.QualitySummaryResponse;
import com.foodsupply.quality.service.QualitySummaryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Quality Summary")
@RestController
@RequestMapping("/api/v1/quality")
@RequiredArgsConstructor
public class QualitySummaryController {

    private final QualitySummaryService service;

    @GetMapping("/summary")
    public QualitySummaryResponse summary() {
        return service.summary();
    }
}

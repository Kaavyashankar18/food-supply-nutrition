package com.foodsupply.quality.controller;

import com.foodsupply.quality.dto.InspectionRequest;
import com.foodsupply.quality.dto.InspectionResponse;
import com.foodsupply.quality.dto.PageResponse;
import com.foodsupply.quality.entity.InspectionResult;
import com.foodsupply.quality.entity.QualityStatus;
import com.foodsupply.quality.service.InspectionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "Quality Inspections")
@RestController
@RequestMapping("/api/v1/inspections")
@RequiredArgsConstructor
public class InspectionController {

    private final InspectionService service;

    @GetMapping
    public PageResponse<InspectionResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) InspectionResult result,
            @RequestParam(required = false) QualityStatus qualityStatus,
            @RequestParam(required = false) Long foodItemId,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "asc") String dir) {
        return service.list(q, result, qualityStatus, foodItemId, batchId, from, to, page, size, sort, dir);
    }

    @GetMapping("/{id}")
    public InspectionResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<InspectionResponse> create(@Valid @RequestBody InspectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }
}

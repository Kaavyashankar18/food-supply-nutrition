package com.foodsupply.quality.controller;

import com.foodsupply.quality.dto.PageResponse;
import com.foodsupply.quality.dto.WastageRequest;
import com.foodsupply.quality.dto.WastageResponse;
import com.foodsupply.quality.dto.WastageSummaryResponse;
import com.foodsupply.quality.entity.WastageReason;
import com.foodsupply.quality.service.WastageService;
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

@Tag(name = "Wastage")
@RestController
@RequestMapping("/api/v1/wastage")
@RequiredArgsConstructor
public class WastageController {

    private final WastageService service;

    @GetMapping
    public PageResponse<WastageResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) WastageReason reason,
            @RequestParam(required = false) Long foodItemId,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "asc") String dir) {
        return service.list(q, reason, foodItemId, batchId, from, to, page, size, sort, dir);
    }

    @GetMapping("/summary")
    public WastageSummaryResponse summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.summary(from, to);
    }

    @GetMapping("/{id}")
    public WastageResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<WastageResponse> create(@Valid @RequestBody WastageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }
}

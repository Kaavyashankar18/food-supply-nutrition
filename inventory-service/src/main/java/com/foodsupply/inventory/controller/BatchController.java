package com.foodsupply.inventory.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.foodsupply.inventory.dto.BatchResponse;
import com.foodsupply.inventory.dto.PageResponse;
import com.foodsupply.inventory.dto.WriteOffRequest;
import com.foodsupply.inventory.entity.QualityStatus;
import com.foodsupply.inventory.service.BatchService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/batches")
@RequiredArgsConstructor
@Tag(name = "Batches")
public class BatchController {

    private final BatchService service;

    @GetMapping
    public PageResponse<BatchResponse> list(@RequestParam(required = false) Long foodItemId,
            @RequestParam(required = false) QualityStatus qualityStatus,
            @RequestParam(required = false) Long supplierId, @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean expired,
            @RequestParam(required = false) Integer expiringWithinDays,
            @RequestParam(required = false) Boolean inStock, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(defaultValue = "expiryDate") String sort,
            @RequestParam(defaultValue = "asc") String dir) {
        return service.search(foodItemId, qualityStatus, supplierId, q, expired, expiringWithinDays, inStock, page,
                size, sort, dir);
    }

    @GetMapping("/{id}")
    public BatchResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PatchMapping("/{id}/quality-status")
    public BatchResponse updateQualityStatus(@PathVariable Long id, @RequestParam("value") QualityStatus value,
            @RequestParam(required = false) String remarks) {
        return service.updateQualityStatus(id, value, remarks);
    }

    @PostMapping("/{id}/write-off")
    public BatchResponse writeOff(@PathVariable Long id, @Valid @RequestBody WriteOffRequest request) {
        return service.writeOff(id, request);
    }
}

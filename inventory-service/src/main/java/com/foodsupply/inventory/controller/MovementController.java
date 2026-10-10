package com.foodsupply.inventory.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.foodsupply.inventory.dto.MovementResponse;
import com.foodsupply.inventory.dto.PageResponse;
import com.foodsupply.inventory.entity.MovementType;
import com.foodsupply.inventory.service.MovementService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/movements")
@RequiredArgsConstructor
@Tag(name = "Stock Movements")
public class MovementController {

    private final MovementService service;

    @GetMapping
    public PageResponse<MovementResponse> list(@RequestParam(required = false) Long foodItemId,
            @RequestParam(required = false) Long batchId, @RequestParam(required = false) MovementType type,
            @RequestParam(required = false) String referenceType,
            @RequestParam(required = false) Long referenceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String dir) {
        return service.search(foodItemId, batchId, type, referenceType, referenceId, from, to, page, size, sort,
                dir);
    }
}

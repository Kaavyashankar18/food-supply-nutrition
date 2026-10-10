package com.foodsupply.foodnutrition.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.foodsupply.foodnutrition.dto.FoodItemRequest;
import com.foodsupply.foodnutrition.dto.FoodItemResponse;
import com.foodsupply.foodnutrition.dto.FoodStatsResponse;
import com.foodsupply.foodnutrition.dto.PageResponse;
import com.foodsupply.foodnutrition.entity.FoodUnit;
import com.foodsupply.foodnutrition.service.FoodItemService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/food-items")
@RequiredArgsConstructor
@Tag(name = "Food Items")
public class FoodItemController {

    private final FoodItemService service;

    @GetMapping
    public PageResponse<FoodItemResponse> list(@RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId, @RequestParam(required = false) FoodUnit unit,
            @RequestParam(required = false) Boolean active, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "asc") String dir) {
        return service.search(q, categoryId, unit, active, page, size, sort, dir);
    }

    @GetMapping("/by-ids")
    public List<FoodItemResponse> byIds(@RequestParam List<Long> ids) {
        return service.getByIds(ids);
    }

    @GetMapping("/stats")
    public FoodStatsResponse stats() {
        return service.stats();
    }

    @GetMapping("/{id}")
    public FoodItemResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<FoodItemResponse> create(@Valid @RequestBody FoodItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public FoodItemResponse update(@PathVariable Long id, @Valid @RequestBody FoodItemRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/active")
    public FoodItemResponse setActive(@PathVariable Long id, @RequestParam boolean value) {
        return service.setActive(id, value);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}

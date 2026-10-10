package com.foodsupply.foodnutrition.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.foodsupply.foodnutrition.dto.FoodCategoryRequest;
import com.foodsupply.foodnutrition.dto.FoodCategoryResponse;
import com.foodsupply.foodnutrition.dto.PageResponse;
import com.foodsupply.foodnutrition.service.FoodCategoryService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/food-categories")
@RequiredArgsConstructor
@Tag(name = "Food Categories")
public class FoodCategoryController {

    private final FoodCategoryService service;

    @GetMapping
    public PageResponse<FoodCategoryResponse> list(@RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sort, @RequestParam(defaultValue = "asc") String dir) {
        return service.search(q, page, size, sort, dir);
    }

    @GetMapping("/all")
    public List<FoodCategoryResponse> all() {
        return service.listAll();
    }

    @GetMapping("/{id}")
    public FoodCategoryResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<FoodCategoryResponse> create(@Valid @RequestBody FoodCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public FoodCategoryResponse update(@PathVariable Long id, @Valid @RequestBody FoodCategoryRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

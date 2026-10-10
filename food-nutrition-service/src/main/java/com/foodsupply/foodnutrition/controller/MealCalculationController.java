package com.foodsupply.foodnutrition.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.foodsupply.foodnutrition.dto.MealCalculationRequest;
import com.foodsupply.foodnutrition.dto.MealCalculationResponse;
import com.foodsupply.foodnutrition.service.MealCalculatorService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/meal-calculations")
@RequiredArgsConstructor
@Tag(name = "Meal Calculator")
public class MealCalculationController {

    private final MealCalculatorService service;

    @PostMapping
    public MealCalculationResponse calculate(@Valid @RequestBody MealCalculationRequest request) {
        return service.calculate(request);
    }
}

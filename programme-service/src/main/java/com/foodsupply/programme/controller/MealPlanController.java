package com.foodsupply.programme.controller;

import com.foodsupply.programme.dto.MealPlanRequest;
import com.foodsupply.programme.dto.MealPlanResponse;
import com.foodsupply.programme.service.MealPlanService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Meal Plans")
@RestController
@RequestMapping("/api/v1/programmes/{programmeId}/meal-plans")
@RequiredArgsConstructor
public class MealPlanController {

    private final MealPlanService mealPlanService;

    @GetMapping
    public List<MealPlanResponse> list(@PathVariable Long programmeId) {
        return mealPlanService.list(programmeId);
    }

    @PostMapping
    public ResponseEntity<MealPlanResponse> create(@PathVariable Long programmeId,
                                                   @Valid @RequestBody MealPlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mealPlanService.create(programmeId, request));
    }

    @PutMapping("/{mealPlanId}")
    public MealPlanResponse update(@PathVariable Long programmeId, @PathVariable Long mealPlanId,
                                   @Valid @RequestBody MealPlanRequest request) {
        return mealPlanService.update(programmeId, mealPlanId, request);
    }

    @DeleteMapping("/{mealPlanId}")
    public ResponseEntity<Void> delete(@PathVariable Long programmeId, @PathVariable Long mealPlanId) {
        mealPlanService.delete(programmeId, mealPlanId);
        return ResponseEntity.noContent().build();
    }
}

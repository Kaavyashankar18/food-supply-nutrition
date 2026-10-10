package com.foodsupply.beneficiary.controller;

import com.foodsupply.beneficiary.dto.NutritionRequirementDto;
import com.foodsupply.beneficiary.entity.BeneficiaryCategory;
import com.foodsupply.beneficiary.service.NutritionRequirementService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/nutrition-requirements")
@RequiredArgsConstructor
@Tag(name = "Nutrition requirements")
public class NutritionRequirementController {

    private final NutritionRequirementService service;

    @GetMapping
    public List<NutritionRequirementDto> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public NutritionRequirementDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/category/{category}")
    public NutritionRequirementDto byCategory(@PathVariable BeneficiaryCategory category) {
        return service.getByCategory(category);
    }

    @PostMapping
    public ResponseEntity<NutritionRequirementDto> create(@Valid @RequestBody NutritionRequirementDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PutMapping("/{id}")
    public NutritionRequirementDto update(@PathVariable Long id, @Valid @RequestBody NutritionRequirementDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}

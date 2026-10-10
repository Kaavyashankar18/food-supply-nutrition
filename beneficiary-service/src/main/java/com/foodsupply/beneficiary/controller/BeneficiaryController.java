package com.foodsupply.beneficiary.controller;

import com.foodsupply.beneficiary.dto.BeneficiaryDto;
import com.foodsupply.beneficiary.dto.CategorySummary;
import com.foodsupply.beneficiary.dto.EligibilityResponse;
import com.foodsupply.beneficiary.dto.PageResponse;
import com.foodsupply.beneficiary.entity.BeneficiaryCategory;
import com.foodsupply.beneficiary.entity.BeneficiaryStatus;
import com.foodsupply.beneficiary.entity.BeneficiaryType;
import com.foodsupply.beneficiary.service.BeneficiaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/beneficiaries")
@RequiredArgsConstructor
@Tag(name = "Beneficiaries")
public class BeneficiaryController {

    private final BeneficiaryService service;

    @GetMapping
    @Operation(summary = "Search beneficiaries with paging, filters and free-text search")
    public PageResponse<BeneficiaryDto> search(@RequestParam(required = false) String q,
                                               @RequestParam(required = false) BeneficiaryCategory category,
                                               @RequestParam(required = false) BeneficiaryType type,
                                               @RequestParam(required = false) BeneficiaryStatus status,
                                               @RequestParam(required = false) Boolean eligible,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "10") int size,
                                               @RequestParam(defaultValue = "id") String sort,
                                               @RequestParam(defaultValue = "asc") String dir) {
        Sort.Direction direction = "desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        int safeSize = Math.min(Math.max(size, 1), 1000);
        return service.search(q, category, type, status, eligible,
                PageRequest.of(Math.max(page, 0), safeSize, Sort.by(direction, sortField(sort))));
    }

    @GetMapping("/{id}")
    public BeneficiaryDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<BeneficiaryDto> create(@Valid @RequestBody BeneficiaryDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PutMapping("/{id}")
    public BeneficiaryDto update(@PathVariable Long id, @Valid @RequestBody BeneficiaryDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/{id}/eligibility")
    @Operation(summary = "Evaluate eligibility without saving")
    public EligibilityResponse eligibility(@PathVariable Long id) {
        return service.checkEligibility(id);
    }

    @PostMapping("/{id}/verify")
    @Operation(summary = "Evaluate eligibility, save the outcome and stamp the verification date")
    public EligibilityResponse verify(@PathVariable Long id) {
        return service.verifyEligibility(id);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activate or deactivate a beneficiary")
    public BeneficiaryDto changeStatus(@PathVariable Long id, @RequestParam BeneficiaryStatus value) {
        return service.changeStatus(id, value);
    }

    @GetMapping("/by-ids")
    @Operation(summary = "Fetch several beneficiaries by id (used by other services)")
    public List<BeneficiaryDto> byIds(@RequestParam List<Long> ids) {
        return service.getByIds(ids);
    }

    @GetMapping("/summary")
    @Operation(summary = "People covered per category (active + eligible) with daily nutrition requirement")
    public List<CategorySummary> summary() {
        return service.categorySummary();
    }

    @GetMapping("/stats")
    public Map<String, Long> stats() {
        return service.stats();
    }

    /** Whitelist of sortable fields so the sort parameter cannot reference arbitrary properties. */
    private String sortField(String requested) {
        return switch (requested) {
            case "fullName", "age", "category", "type", "peopleServed", "status", "registeredOn", "eligible" -> requested;
            default -> "id";
        };
    }
}

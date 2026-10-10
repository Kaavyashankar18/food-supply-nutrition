package com.foodsupply.distribution.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.foodsupply.distribution.dto.DeliveryRequest;
import com.foodsupply.distribution.dto.DistributionSummary;
import com.foodsupply.distribution.dto.PageResponse;
import com.foodsupply.distribution.dto.PlanListItem;
import com.foodsupply.distribution.dto.PlanRequest;
import com.foodsupply.distribution.dto.PlanResponse;
import com.foodsupply.distribution.dto.PreviewResponse;
import com.foodsupply.distribution.dto.RaiseProcurementRequest;
import com.foodsupply.distribution.entity.PlanStatus;
import com.foodsupply.distribution.service.PlanService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Distribution Plans")
@RestController
@RequestMapping("/api/v1/distribution-plans")
@RequiredArgsConstructor
public class DistributionPlanController {

    private final PlanService planService;

    @GetMapping
    public PageResponse<PlanListItem> list(@RequestParam(required = false) String q,
                                           @RequestParam(required = false) PlanStatus status,
                                           @RequestParam(required = false) Long programmeId,
                                           @RequestParam(required = false) Long centreId,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size,
                                           @RequestParam(defaultValue = "id") String sort,
                                           @RequestParam(defaultValue = "desc") String dir) {
        return planService.list(q, status, programmeId, centreId, page, size, sort, dir);
    }

    @GetMapping("/summary")
    public DistributionSummary summary() {
        return planService.summary();
    }

    @PostMapping("/generate")
    public PreviewResponse generate(@Valid @RequestBody PlanRequest request) {
        return planService.preview(request);
    }

    @PostMapping("/raise-procurement")
    public ResponseEntity<JsonNode> raiseProcurement(@Valid @RequestBody RaiseProcurementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planService.raiseProcurement(request));
    }

    @PostMapping
    public ResponseEntity<PlanResponse> create(@Valid @RequestBody PlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planService.create(request));
    }

    @GetMapping("/{id}")
    public PlanResponse get(@PathVariable Long id) {
        return planService.get(id);
    }

    @PostMapping("/{id}/dispatch")
    public PlanResponse dispatch(@PathVariable Long id) {
        return planService.dispatch(id);
    }

    @PostMapping("/{id}/deliver")
    public PlanResponse deliver(@PathVariable Long id, @Valid @RequestBody(required = false) DeliveryRequest request) {
        return planService.deliver(id, request);
    }

    @PostMapping("/{id}/cancel")
    public PlanResponse cancel(@PathVariable Long id) {
        return planService.cancel(id);
    }
}

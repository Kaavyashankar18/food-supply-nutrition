package com.foodsupply.quality.controller;

import com.foodsupply.quality.dto.ActionRequest;
import com.foodsupply.quality.dto.ActionResponse;
import com.foodsupply.quality.dto.PageResponse;
import com.foodsupply.quality.entity.ActionStatus;
import com.foodsupply.quality.service.ActionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

@Tag(name = "Corrective Actions")
@RestController
@RequestMapping("/api/v1/corrective-actions")
@RequiredArgsConstructor
public class ActionController {

    private final ActionService service;

    @GetMapping
    public PageResponse<ActionResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long inspectionId,
            @RequestParam(required = false) Long wastageId,
            @RequestParam(required = false) ActionStatus status,
            @RequestParam(required = false) Boolean overdue,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "asc") String dir) {
        return service.list(q, inspectionId, wastageId, status, overdue, page, size, sort, dir);
    }

    @GetMapping("/{id}")
    public ActionResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<ActionResponse> create(@Valid @RequestBody ActionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public ActionResponse update(@PathVariable Long id, @Valid @RequestBody ActionRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public ActionResponse changeStatus(@PathVariable Long id, @RequestParam("value") ActionStatus value) {
        return service.changeStatus(id, value);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

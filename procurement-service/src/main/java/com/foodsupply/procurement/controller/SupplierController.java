package com.foodsupply.procurement.controller;

import com.foodsupply.procurement.dto.PageResponse;
import com.foodsupply.procurement.dto.SupplierRequest;
import com.foodsupply.procurement.dto.SupplierResponse;
import com.foodsupply.procurement.entity.SupplierStatus;
import com.foodsupply.procurement.service.SupplierService;
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

@Tag(name = "Suppliers")
@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    public PageResponse<SupplierResponse> list(@RequestParam(required = false) String q,
                                               @RequestParam(required = false) SupplierStatus status,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size,
                                               @RequestParam(required = false) String sort,
                                               @RequestParam(required = false) String dir) {
        return supplierService.list(q, status, page, size, sort, dir);
    }

    @GetMapping("/{id}")
    public SupplierResponse get(@PathVariable Long id) {
        return supplierService.get(id);
    }

    @PostMapping
    public ResponseEntity<SupplierResponse> create(@Valid @RequestBody SupplierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierService.create(request));
    }

    @PutMapping("/{id}")
    public SupplierResponse update(@PathVariable Long id, @Valid @RequestBody SupplierRequest request) {
        return supplierService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        supplierService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public SupplierResponse updateStatus(@PathVariable Long id, @RequestParam SupplierStatus value) {
        return supplierService.updateStatus(id, value);
    }
}

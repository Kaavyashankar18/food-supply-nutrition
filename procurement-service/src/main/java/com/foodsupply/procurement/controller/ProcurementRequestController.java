package com.foodsupply.procurement.controller;

import com.foodsupply.procurement.dto.PageResponse;
import com.foodsupply.procurement.dto.ProcurementRequestInput;
import com.foodsupply.procurement.dto.RequestResponse;
import com.foodsupply.procurement.entity.RequestSource;
import com.foodsupply.procurement.entity.RequestStatus;
import com.foodsupply.procurement.service.ProcurementRequestService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

@Tag(name = "Procurement Requests")
@RestController
@RequestMapping("/api/v1/procurement-requests")
@RequiredArgsConstructor
public class ProcurementRequestController {

    private final ProcurementRequestService requestService;

    @GetMapping
    public PageResponse<RequestResponse> list(@RequestParam(required = false) String q,
                                              @RequestParam(required = false) RequestStatus status,
                                              @RequestParam(required = false) RequestSource source,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size,
                                              @RequestParam(required = false) String sort,
                                              @RequestParam(required = false) String dir) {
        return requestService.list(q, status, source, page, size, sort, dir);
    }

    @GetMapping("/{id}")
    public RequestResponse get(@PathVariable Long id) {
        return requestService.get(id);
    }

    @PostMapping
    public ResponseEntity<RequestResponse> create(@Valid @RequestBody ProcurementRequestInput input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(requestService.create(input));
    }

    @PutMapping("/{id}")
    public RequestResponse update(@PathVariable Long id, @Valid @RequestBody ProcurementRequestInput input) {
        return requestService.update(id, input);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        requestService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/submit")
    public RequestResponse submit(@PathVariable Long id) {
        return requestService.submit(id);
    }

    @PostMapping("/{id}/approve")
    public RequestResponse approve(@PathVariable Long id) {
        return requestService.approve(id);
    }

    @PostMapping("/{id}/reject")
    public RequestResponse reject(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return requestService.reject(id, reason);
    }
}

package com.foodsupply.inventory.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.foodsupply.inventory.dto.AvailabilityRequest;
import com.foodsupply.inventory.dto.AvailabilityResponse;
import com.foodsupply.inventory.dto.BatchResponse;
import com.foodsupply.inventory.dto.PageResponse;
import com.foodsupply.inventory.dto.ReceiveStockRequest;
import com.foodsupply.inventory.dto.ReferenceRequest;
import com.foodsupply.inventory.dto.ReservationResponse;
import com.foodsupply.inventory.dto.ReservationResultResponse;
import com.foodsupply.inventory.dto.ReserveRequest;
import com.foodsupply.inventory.dto.StockLevelResponse;
import com.foodsupply.inventory.dto.StockSummaryResponse;
import com.foodsupply.inventory.service.StockService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Stock")
public class StockController {

    private final StockService service;

    @GetMapping("/stock")
    public PageResponse<StockLevelResponse> levels(@RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean lowStock, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(defaultValue = "foodItemName") String sort,
            @RequestParam(defaultValue = "asc") String dir) {
        return service.levels(q, lowStock, page, size, sort, dir);
    }

    @GetMapping("/stock/summary")
    public StockSummaryResponse summary() {
        return service.summary();
    }

    @GetMapping("/stock/{foodItemId}")
    public StockLevelResponse level(@PathVariable Long foodItemId) {
        return service.level(foodItemId);
    }

    @PatchMapping("/stock/{foodItemId}/minimum-level")
    public StockLevelResponse setMinimumLevel(@PathVariable Long foodItemId, @RequestParam BigDecimal value) {
        return service.setMinimumLevel(foodItemId, value);
    }

    @PostMapping("/stock/receive")
    public ResponseEntity<BatchResponse> receive(@Valid @RequestBody ReceiveStockRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.receive(request));
    }

    @PostMapping("/stock/availability")
    public AvailabilityResponse availability(@Valid @RequestBody AvailabilityRequest request) {
        return service.availability(request);
    }

    @PostMapping("/stock/reserve")
    public ReservationResultResponse reserve(@Valid @RequestBody ReserveRequest request) {
        return service.reserve(request);
    }

    @PostMapping("/stock/release")
    public ReservationResultResponse release(@Valid @RequestBody ReferenceRequest request) {
        return service.release(request);
    }

    @PostMapping("/stock/dispatch")
    public ReservationResultResponse dispatch(@Valid @RequestBody ReferenceRequest request) {
        return service.dispatch(request);
    }

    @GetMapping("/reservations")
    public List<ReservationResponse> reservations(@RequestParam String referenceType,
            @RequestParam Long referenceId) {
        return service.reservations(referenceType, referenceId);
    }
}

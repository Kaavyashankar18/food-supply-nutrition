package com.foodsupply.procurement.controller;

import com.foodsupply.procurement.dto.PageResponse;
import com.foodsupply.procurement.dto.PurchaseOrderInput;
import com.foodsupply.procurement.dto.PurchaseOrderResponse;
import com.foodsupply.procurement.dto.ReceiveInput;
import com.foodsupply.procurement.entity.PurchaseOrderStatus;
import com.foodsupply.procurement.service.PurchaseOrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

@Tag(name = "Purchase Orders")
@RestController
@RequestMapping("/api/v1/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService orderService;

    @GetMapping
    public PageResponse<PurchaseOrderResponse> list(@RequestParam(required = false) String q,
                                                    @RequestParam(required = false) PurchaseOrderStatus status,
                                                    @RequestParam(required = false) Long supplierId,
                                                    @RequestParam(required = false) Long requestId,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size,
                                                    @RequestParam(required = false) String sort,
                                                    @RequestParam(required = false) String dir) {
        return orderService.list(q, status, supplierId, requestId, page, size, sort, dir);
    }

    @GetMapping("/{id}")
    public PurchaseOrderResponse get(@PathVariable Long id) {
        return orderService.get(id);
    }

    @PostMapping
    public ResponseEntity<PurchaseOrderResponse> create(@Valid @RequestBody PurchaseOrderInput input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(input));
    }

    @PostMapping("/{id}/receive")
    public PurchaseOrderResponse receive(@PathVariable Long id, @Valid @RequestBody ReceiveInput input) {
        return orderService.receive(id, input);
    }

    @PatchMapping("/{id}/status")
    public PurchaseOrderResponse updateStatus(@PathVariable Long id, @RequestParam PurchaseOrderStatus value) {
        return orderService.updateStatus(id, value);
    }
}

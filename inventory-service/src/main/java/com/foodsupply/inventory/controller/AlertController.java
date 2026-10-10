package com.foodsupply.inventory.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.foodsupply.inventory.dto.BatchResponse;
import com.foodsupply.inventory.dto.StockLevelResponse;
import com.foodsupply.inventory.service.BatchService;
import com.foodsupply.inventory.service.StockService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@Tag(name = "Alerts")
public class AlertController {

    private final StockService stockService;
    private final BatchService batchService;

    @GetMapping("/low-stock")
    public List<StockLevelResponse> lowStock() {
        return stockService.lowStock();
    }

    @GetMapping("/expiring")
    public List<BatchResponse> expiring(@RequestParam(defaultValue = "7") int days) {
        return batchService.expiring(days);
    }

    @GetMapping("/expired")
    public List<BatchResponse> expired() {
        return batchService.expired();
    }
}

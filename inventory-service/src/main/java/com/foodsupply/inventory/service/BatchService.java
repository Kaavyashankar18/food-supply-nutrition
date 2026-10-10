package com.foodsupply.inventory.service;

import java.util.List;

import com.foodsupply.inventory.dto.BatchResponse;
import com.foodsupply.inventory.dto.PageResponse;
import com.foodsupply.inventory.dto.WriteOffRequest;
import com.foodsupply.inventory.entity.QualityStatus;

public interface BatchService {

    PageResponse<BatchResponse> search(Long foodItemId, QualityStatus qualityStatus, Long supplierId, String q,
            Boolean expired, Integer expiringWithinDays, Boolean inStock, int page, int size, String sort,
            String dir);

    BatchResponse get(Long id);

    BatchResponse updateQualityStatus(Long id, QualityStatus status, String remarks);

    BatchResponse writeOff(Long id, WriteOffRequest request);

    List<BatchResponse> expiring(int days);

    List<BatchResponse> expired();
}

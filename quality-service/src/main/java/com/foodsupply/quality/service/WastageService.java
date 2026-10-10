package com.foodsupply.quality.service;

import com.foodsupply.quality.dto.PageResponse;
import com.foodsupply.quality.dto.WastageRequest;
import com.foodsupply.quality.dto.WastageResponse;
import com.foodsupply.quality.dto.WastageSummaryResponse;
import com.foodsupply.quality.entity.WastageReason;

import java.time.LocalDate;

public interface WastageService {

    PageResponse<WastageResponse> list(String q, WastageReason reason, Long foodItemId, Long batchId,
                                       LocalDate from, LocalDate to, int page, int size, String sort, String dir);

    WastageResponse get(Long id);

    WastageResponse create(WastageRequest request);

    WastageSummaryResponse summary(LocalDate from, LocalDate to);
}

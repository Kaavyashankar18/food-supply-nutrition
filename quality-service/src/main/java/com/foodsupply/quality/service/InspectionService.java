package com.foodsupply.quality.service;

import com.foodsupply.quality.dto.InspectionRequest;
import com.foodsupply.quality.dto.InspectionResponse;
import com.foodsupply.quality.dto.PageResponse;
import com.foodsupply.quality.entity.InspectionResult;
import com.foodsupply.quality.entity.QualityStatus;

import java.time.LocalDate;

public interface InspectionService {

    PageResponse<InspectionResponse> list(String q, InspectionResult result, QualityStatus qualityStatus,
                                          Long foodItemId, Long batchId, LocalDate from, LocalDate to,
                                          int page, int size, String sort, String dir);

    InspectionResponse get(Long id);

    InspectionResponse create(InspectionRequest request);
}

package com.foodsupply.distribution.service;

import com.foodsupply.distribution.dto.PageResponse;
import com.foodsupply.distribution.dto.RecordResponse;
import com.foodsupply.distribution.entity.RecordStatus;

public interface RecordService {

    PageResponse<RecordResponse> list(Long beneficiaryId, Long planId, Long foodItemId, RecordStatus status,
                                      int page, int size, String sort, String dir);
}

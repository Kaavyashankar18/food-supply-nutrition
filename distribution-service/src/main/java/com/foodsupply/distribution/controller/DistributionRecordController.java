package com.foodsupply.distribution.controller;

import com.foodsupply.distribution.dto.PageResponse;
import com.foodsupply.distribution.dto.RecordResponse;
import com.foodsupply.distribution.entity.RecordStatus;
import com.foodsupply.distribution.service.RecordService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Distribution History")
@RestController
@RequestMapping("/api/v1/distribution-records")
@RequiredArgsConstructor
public class DistributionRecordController {

    private final RecordService recordService;

    /** Per-beneficiary distribution history: use ?beneficiaryId=. */
    @GetMapping
    public PageResponse<RecordResponse> list(@RequestParam(required = false) Long beneficiaryId,
                                             @RequestParam(required = false) Long planId,
                                             @RequestParam(required = false) Long foodItemId,
                                             @RequestParam(required = false) RecordStatus status,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             @RequestParam(defaultValue = "id") String sort,
                                             @RequestParam(defaultValue = "desc") String dir) {
        return recordService.list(beneficiaryId, planId, foodItemId, status, page, size, sort, dir);
    }
}

package com.foodsupply.distribution.service;

import com.foodsupply.distribution.dto.PageResponse;
import com.foodsupply.distribution.dto.RecordResponse;
import com.foodsupply.distribution.entity.DistributionRecord;
import com.foodsupply.distribution.entity.RecordStatus;
import com.foodsupply.distribution.mapper.PlanMapper;
import com.foodsupply.distribution.repository.RecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecordServiceImpl implements RecordService {

    private static final Set<String> SORT_FIELDS = Set.of("id", "deliveredOn", "quantity", "beneficiaryId", "foodItemId", "status");

    private final RecordRepository recordRepository;

    @Override
    public PageResponse<RecordResponse> list(Long beneficiaryId, Long planId, Long foodItemId, RecordStatus status,
                                             int page, int size, String sort, String dir) {
        Specification<DistributionRecord> spec = (root, query, cb) -> cb.conjunction();
        if (beneficiaryId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("beneficiaryId"), beneficiaryId));
        }
        if (planId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("plan").get("id"), planId));
        }
        if (foodItemId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("foodItemId"), foodItemId));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        String field = SORT_FIELDS.contains(sort) ? sort : "id";
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 1000), Sort.by(direction, field));
        return PageResponse.of(recordRepository.findAll(spec, pageable).map(PlanMapper::toRecord));
    }
}

package com.foodsupply.quality.service;

import com.foodsupply.quality.client.InventoryClient;
import com.foodsupply.quality.dto.BatchInfo;
import com.foodsupply.quality.dto.InspectionRequest;
import com.foodsupply.quality.dto.InspectionResponse;
import com.foodsupply.quality.dto.PageResponse;
import com.foodsupply.quality.entity.InspectionResult;
import com.foodsupply.quality.entity.QualityInspection;
import com.foodsupply.quality.entity.QualityStatus;
import com.foodsupply.quality.exception.DownstreamServiceException;
import com.foodsupply.quality.exception.InvalidRequestException;
import com.foodsupply.quality.exception.ResourceNotFoundException;
import com.foodsupply.quality.mapper.QualityMapper;
import com.foodsupply.quality.repository.QualityInspectionRepository;
import com.foodsupply.quality.util.PageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InspectionServiceImpl implements InspectionService {

    private static final Set<String> SORT_FIELDS =
            Set.of("id", "inspectionDate", "foodItemName", "result", "qualityStatus", "inspector");

    private final QualityInspectionRepository repository;
    private final InventoryClient inventoryClient;

    @Override
    public PageResponse<InspectionResponse> list(String q, InspectionResult result, QualityStatus qualityStatus,
                                                 Long foodItemId, Long batchId, LocalDate from, LocalDate to,
                                                 int page, int size, String sort, String dir) {
        Specification<QualityInspection> spec = (root, query, cb) -> cb.conjunction();

        if (q != null && !q.isBlank()) {
            final String pattern = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("foodItemName")), pattern),
                    cb.like(cb.lower(root.get("batchNumber")), pattern),
                    cb.like(cb.lower(root.get("inspector")), pattern)));
        }
        if (result != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("result"), result));
        }
        if (qualityStatus != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("qualityStatus"), qualityStatus));
        }
        if (foodItemId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("foodItemId"), foodItemId));
        }
        if (batchId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("batchId"), batchId));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.<LocalDate>get("inspectionDate"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.<LocalDate>get("inspectionDate"), to));
        }

        Pageable pageable = PageUtil.build(page, size, sort, dir, SORT_FIELDS, "inspectionDate", Sort.Direction.DESC);
        return PageResponse.of(repository.findAll(spec, pageable).map(QualityMapper::toInspectionResponse));
    }

    @Override
    public InspectionResponse get(Long id) {
        return QualityMapper.toInspectionResponse(find(id));
    }

    /**
     * Applies the quality status to the batch in inventory FIRST. The inspection is only
     * saved when inventory accepted the change, so both services stay consistent.
     */
    @Override
    @Transactional
    public InspectionResponse create(InspectionRequest request) {
        QualityStatus status = request.getQualityStatus() != null
                ? request.getQualityStatus()
                : defaultStatus(request.getResult());

        if (request.getResult() == InspectionResult.FAIL && status == QualityStatus.APPROVED) {
            throw new InvalidRequestException("A FAILED inspection cannot leave the batch APPROVED");
        }

        BatchInfo batch = inventoryClient.updateQualityStatus(request.getBatchId(), status, buildRemarks(request));
        if (batch == null || batch.foodItemId() == null) {
            throw new DownstreamServiceException("inventory-service returned an unexpected answer for batch "
                    + request.getBatchId());
        }

        QualityInspection saved = repository.save(QualityInspection.builder()
                .inspectionDate(request.getInspectionDate() != null ? request.getInspectionDate() : LocalDate.now())
                .foodItemId(batch.foodItemId())
                .foodItemName(batch.foodItemName())
                .batchId(request.getBatchId())
                .batchNumber(batch.batchNumber())
                .inspector(request.getInspector().trim())
                .result(request.getResult())
                .qualityStatus(status)
                .remarks(request.getRemarks())
                .build());

        log.info("Inspection {} saved: batch {} ({}) -> {} ({})", saved.getId(), saved.getBatchId(),
                saved.getBatchNumber(), saved.getQualityStatus(), saved.getResult());
        return QualityMapper.toInspectionResponse(saved);
    }

    private QualityInspection find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection not found with id " + id));
    }

    private QualityStatus defaultStatus(InspectionResult result) {
        return switch (result) {
            case PASS -> QualityStatus.APPROVED;
            case FAIL -> QualityStatus.REJECTED;
            case CONDITIONAL -> QualityStatus.QUARANTINED;
        };
    }

    private String buildRemarks(InspectionRequest request) {
        String base = "Quality inspection by " + request.getInspector().trim() + " (" + request.getResult() + ")";
        if (request.getRemarks() != null && !request.getRemarks().isBlank()) {
            return base + ": " + request.getRemarks().trim();
        }
        return base;
    }
}

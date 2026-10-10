package com.foodsupply.quality.service;

import com.foodsupply.quality.dto.ActionRequest;
import com.foodsupply.quality.dto.ActionResponse;
import com.foodsupply.quality.dto.PageResponse;
import com.foodsupply.quality.entity.ActionStatus;
import com.foodsupply.quality.entity.CorrectiveAction;
import com.foodsupply.quality.exception.InvalidRequestException;
import com.foodsupply.quality.exception.ResourceNotFoundException;
import com.foodsupply.quality.mapper.QualityMapper;
import com.foodsupply.quality.repository.CorrectiveActionRepository;
import com.foodsupply.quality.repository.QualityInspectionRepository;
import com.foodsupply.quality.repository.WastageRecordRepository;
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
public class ActionServiceImpl implements ActionService {

    private static final Set<String> SORT_FIELDS = Set.of("id", "dueDate", "status", "createdOn", "assignedTo");

    private final CorrectiveActionRepository repository;
    private final QualityInspectionRepository inspectionRepository;
    private final WastageRecordRepository wastageRepository;

    @Override
    public PageResponse<ActionResponse> list(String q, Long inspectionId, Long wastageId, ActionStatus status,
                                             Boolean overdue, int page, int size, String sort, String dir) {
        Specification<CorrectiveAction> spec = (root, query, cb) -> cb.conjunction();

        if (q != null && !q.isBlank()) {
            final String pattern = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("description")), pattern),
                    cb.like(cb.lower(root.get("assignedTo")), pattern)));
        }
        if (inspectionId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("inspectionId"), inspectionId));
        }
        if (wastageId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("wastageId"), wastageId));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (Boolean.TRUE.equals(overdue)) {
            final LocalDate today = LocalDate.now();
            spec = spec.and((root, query, cb) -> cb.and(
                    cb.notEqual(root.get("status"), ActionStatus.CLOSED),
                    cb.lessThan(root.<LocalDate>get("dueDate"), today)));
        }

        Pageable pageable = PageUtil.build(page, size, sort, dir, SORT_FIELDS, "dueDate", Sort.Direction.ASC);
        return PageResponse.of(repository.findAll(spec, pageable).map(QualityMapper::toActionResponse));
    }

    @Override
    public ActionResponse get(Long id) {
        return QualityMapper.toActionResponse(find(id));
    }

    @Override
    @Transactional
    public ActionResponse create(ActionRequest request) {
        if (request.getInspectionId() == null && request.getWastageId() == null) {
            throw new InvalidRequestException("Either inspectionId or wastageId is required");
        }
        if (request.getInspectionId() != null && !inspectionRepository.existsById(request.getInspectionId())) {
            throw new ResourceNotFoundException("Inspection not found with id " + request.getInspectionId());
        }
        if (request.getWastageId() != null && !wastageRepository.existsById(request.getWastageId())) {
            throw new ResourceNotFoundException("Wastage record not found with id " + request.getWastageId());
        }

        ActionStatus status = request.getStatus() != null ? request.getStatus() : ActionStatus.OPEN;
        CorrectiveAction saved = repository.save(CorrectiveAction.builder()
                .inspectionId(request.getInspectionId())
                .wastageId(request.getWastageId())
                .description(request.getDescription().trim())
                .assignedTo(request.getAssignedTo().trim())
                .dueDate(request.getDueDate())
                .status(status)
                .createdOn(LocalDate.now())
                .completedOn(status == ActionStatus.CLOSED ? LocalDate.now() : null)
                .build());
        log.info("Corrective action {} created ({})", saved.getId(), saved.getStatus());
        return QualityMapper.toActionResponse(saved);
    }

    @Override
    @Transactional
    public ActionResponse update(Long id, ActionRequest request) {
        CorrectiveAction action = find(id);
        action.setDescription(request.getDescription().trim());
        action.setAssignedTo(request.getAssignedTo().trim());
        action.setDueDate(request.getDueDate());
        if (request.getStatus() != null) {
            applyStatus(action, request.getStatus());
        }
        return QualityMapper.toActionResponse(repository.save(action));
    }

    @Override
    @Transactional
    public ActionResponse changeStatus(Long id, ActionStatus status) {
        CorrectiveAction action = find(id);
        applyStatus(action, status);
        return QualityMapper.toActionResponse(repository.save(action));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        repository.delete(find(id));
        log.info("Corrective action {} deleted", id);
    }

    private void applyStatus(CorrectiveAction action, ActionStatus status) {
        action.setStatus(status);
        if (status == ActionStatus.CLOSED) {
            if (action.getCompletedOn() == null) {
                action.setCompletedOn(LocalDate.now());
            }
        } else {
            action.setCompletedOn(null);
        }
    }

    private CorrectiveAction find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Corrective action not found with id " + id));
    }
}

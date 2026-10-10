package com.foodsupply.inventory.service;

import java.time.LocalDate;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.foodsupply.inventory.dto.MovementResponse;
import com.foodsupply.inventory.dto.PageResponse;
import com.foodsupply.inventory.entity.MovementType;
import com.foodsupply.inventory.entity.StockMovement;
import com.foodsupply.inventory.mapper.MovementMapper;
import com.foodsupply.inventory.repository.StockMovementRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovementServiceImpl implements MovementService {

    private static final Set<String> SORT_FIELDS = Set.of("id", "createdAt", "type", "quantity", "foodItemId");

    private final StockMovementRepository movementRepository;

    @Override
    public PageResponse<MovementResponse> search(Long foodItemId, Long batchId, MovementType type,
            String referenceType, Long referenceId, LocalDate from, LocalDate to, int page, int size, String sort,
            String dir) {
        Specification<StockMovement> spec = (root, query, cb) -> cb.conjunction();
        if (foodItemId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("foodItemId"), foodItemId));
        }
        if (batchId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("batchId"), batchId));
        }
        if (type != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), type));
        }
        if (referenceType != null && !referenceType.isBlank()) {
            String rt = referenceType.trim().toUpperCase();
            spec = spec.and((root, query, cb) -> cb.equal(root.get("referenceType"), rt));
        }
        if (referenceId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("referenceId"), referenceId));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(
                    root.<java.time.LocalDateTime>get("createdAt"), from.atStartOfDay()));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThan(root.<java.time.LocalDateTime>get("createdAt"),
                    to.plusDays(1).atStartOfDay()));
        }
        String sortField = SORT_FIELDS.contains(sort) ? sort : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 1000),
                Sort.by(direction, sortField).and(Sort.by(direction, "id")));
        Page<StockMovement> result = movementRepository.findAll(spec, pageable);
        return new PageResponse<>(result.getContent().stream().map(MovementMapper::toResponse).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
}

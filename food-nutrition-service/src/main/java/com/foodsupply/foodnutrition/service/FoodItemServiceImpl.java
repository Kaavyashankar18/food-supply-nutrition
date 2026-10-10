package com.foodsupply.foodnutrition.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.foodsupply.foodnutrition.dto.FoodItemRequest;
import com.foodsupply.foodnutrition.dto.FoodItemResponse;
import com.foodsupply.foodnutrition.dto.FoodStatsResponse;
import com.foodsupply.foodnutrition.dto.PageResponse;
import com.foodsupply.foodnutrition.entity.FoodCategory;
import com.foodsupply.foodnutrition.entity.FoodItem;
import com.foodsupply.foodnutrition.entity.FoodUnit;
import com.foodsupply.foodnutrition.exception.BusinessException;
import com.foodsupply.foodnutrition.exception.InvalidRequestException;
import com.foodsupply.foodnutrition.exception.ResourceNotFoundException;
import com.foodsupply.foodnutrition.mapper.FoodItemMapper;
import com.foodsupply.foodnutrition.repository.FoodCategoryRepository;
import com.foodsupply.foodnutrition.repository.FoodItemRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FoodItemServiceImpl implements FoodItemService {

    private static final Set<String> SORT_FIELDS = Set.of("id", "name", "unit", "caloriesPer100g",
            "proteinPer100g", "carbsPer100g", "fatPer100g", "recommendedQtyPerPersonPerDay", "active");

    private final FoodItemRepository itemRepository;
    private final FoodCategoryRepository categoryRepository;

    @Override
    public PageResponse<FoodItemResponse> search(String q, Long categoryId, FoodUnit unit, Boolean active,
            int page, int size, String sort, String dir) {
        Specification<FoodItem> spec = (root, query, cb) -> cb.conjunction();
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), like));
        }
        if (categoryId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId));
        }
        if (unit != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("unit"), unit));
        }
        if (active != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), active));
        }
        String sortField = SORT_FIELDS.contains(sort) ? sort : "name";
        Sort.Direction direction = "desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 1000),
                Sort.by(direction, sortField));
        Page<FoodItem> result = itemRepository.findAll(spec, pageable);
        return PageResponse.of(result.map(FoodItemMapper::toResponse));
    }

    @Override
    public FoodItemResponse get(Long id) {
        return FoodItemMapper.toResponse(find(id));
    }

    @Override
    public List<FoodItemResponse> getByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return itemRepository.findAllById(ids).stream().map(FoodItemMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public FoodItemResponse create(FoodItemRequest request) {
        String name = request.getName().trim();
        if (itemRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException("Food item already exists: " + name);
        }
        FoodCategory category = findCategory(request.getCategoryId());
        FoodItem saved = itemRepository.save(FoodItemMapper.toEntity(request, category));
        log.info("Created food item {} ({})", saved.getName(), saved.getId());
        return FoodItemMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public FoodItemResponse update(Long id, FoodItemRequest request) {
        FoodItem item = find(id);
        String name = request.getName().trim();
        if (itemRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BusinessException("Food item already exists: " + name);
        }
        FoodCategory category = findCategory(request.getCategoryId());
        FoodItemMapper.apply(item, request, category);
        return FoodItemMapper.toResponse(itemRepository.save(item));
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        FoodItem item = find(id);
        item.setActive(false);
        itemRepository.save(item);
        log.info("Deactivated food item {}", id);
    }

    @Override
    @Transactional
    public FoodItemResponse setActive(Long id, boolean active) {
        FoodItem item = find(id);
        item.setActive(active);
        return FoodItemMapper.toResponse(itemRepository.save(item));
    }

    @Override
    public FoodStatsResponse stats() {
        Map<String, Long> perCategory = new LinkedHashMap<>();
        for (Object[] row : itemRepository.countItemsPerCategory()) {
            perCategory.put((String) row[0], ((Number) row[1]).longValue());
        }
        long active = itemRepository.countByActive(true);
        long inactive = itemRepository.countByActive(false);
        return new FoodStatsResponse(active + inactive, active, inactive, categoryRepository.count(), perCategory);
    }

    private FoodItem find(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Food item not found: " + id));
    }

    private FoodCategory findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new InvalidRequestException("Food category not found: " + id));
    }
}

package com.foodsupply.foodnutrition.service;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.foodsupply.foodnutrition.dto.FoodCategoryRequest;
import com.foodsupply.foodnutrition.dto.FoodCategoryResponse;
import com.foodsupply.foodnutrition.dto.PageResponse;
import com.foodsupply.foodnutrition.entity.FoodCategory;
import com.foodsupply.foodnutrition.exception.BusinessException;
import com.foodsupply.foodnutrition.exception.ResourceNotFoundException;
import com.foodsupply.foodnutrition.mapper.FoodCategoryMapper;
import com.foodsupply.foodnutrition.repository.FoodCategoryRepository;
import com.foodsupply.foodnutrition.repository.FoodItemRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FoodCategoryServiceImpl implements FoodCategoryService {

    private static final Set<String> SORT_FIELDS = Set.of("id", "name");

    private final FoodCategoryRepository categoryRepository;
    private final FoodItemRepository itemRepository;

    @Override
    public PageResponse<FoodCategoryResponse> search(String q, int page, int size, String sort, String dir) {
        Specification<FoodCategory> spec = (root, query, cb) -> cb.conjunction();
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), like));
        }
        String sortField = SORT_FIELDS.contains(sort) ? sort : "name";
        Sort.Direction direction = "desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 1000),
                Sort.by(direction, sortField));
        Page<FoodCategory> result = categoryRepository.findAll(spec, pageable);
        return PageResponse.of(result.map(c -> FoodCategoryMapper.toResponse(c,
                itemRepository.countByCategoryId(c.getId()))));
    }

    @Override
    public List<FoodCategoryResponse> listAll() {
        return categoryRepository.findAll(Sort.by("name")).stream()
                .map(c -> FoodCategoryMapper.toResponse(c, itemRepository.countByCategoryId(c.getId())))
                .toList();
    }

    @Override
    public FoodCategoryResponse get(Long id) {
        FoodCategory c = find(id);
        return FoodCategoryMapper.toResponse(c, itemRepository.countByCategoryId(id));
    }

    @Override
    @Transactional
    public FoodCategoryResponse create(FoodCategoryRequest request) {
        String name = request.getName().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException("Food category already exists: " + name);
        }
        FoodCategory saved = categoryRepository.save(FoodCategory.builder().name(name).build());
        log.info("Created food category {} ({})", saved.getName(), saved.getId());
        return FoodCategoryMapper.toResponse(saved, 0);
    }

    @Override
    @Transactional
    public FoodCategoryResponse update(Long id, FoodCategoryRequest request) {
        FoodCategory c = find(id);
        String name = request.getName().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BusinessException("Food category already exists: " + name);
        }
        c.setName(name);
        FoodCategory saved = categoryRepository.save(c);
        return FoodCategoryMapper.toResponse(saved, itemRepository.countByCategoryId(id));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        FoodCategory c = find(id);
        long items = itemRepository.countByCategoryId(id);
        if (items > 0) {
            throw new BusinessException(
                    "Cannot delete category '" + c.getName() + "': it still has " + items + " food item(s)");
        }
        categoryRepository.delete(c);
        log.info("Deleted food category {}", id);
    }

    private FoodCategory find(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Food category not found: " + id));
    }
}

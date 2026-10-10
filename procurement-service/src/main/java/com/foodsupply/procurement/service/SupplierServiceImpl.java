package com.foodsupply.procurement.service;

import com.foodsupply.procurement.dto.PageResponse;
import com.foodsupply.procurement.dto.SupplierRequest;
import com.foodsupply.procurement.dto.SupplierResponse;
import com.foodsupply.procurement.entity.Supplier;
import com.foodsupply.procurement.entity.SupplierStatus;
import com.foodsupply.procurement.exception.BusinessException;
import com.foodsupply.procurement.exception.ResourceNotFoundException;
import com.foodsupply.procurement.mapper.SupplierMapper;
import com.foodsupply.procurement.repository.PurchaseOrderRepository;
import com.foodsupply.procurement.repository.SupplierRepository;
import com.foodsupply.procurement.util.PageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupplierServiceImpl implements SupplierService {

    private static final Set<String> SORT_FIELDS = Set.of("id", "name", "contactPerson", "rating", "status");

    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    @Override
    public PageResponse<SupplierResponse> list(String q, SupplierStatus status, int page, int size,
                                               String sort, String dir) {
        Specification<Supplier> spec = (root, query, cb) -> cb.conjunction();
        if (q != null && !q.isBlank()) {
            String pattern = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.<String>get("name")), pattern),
                    cb.like(cb.lower(root.<String>get("contactPerson")), pattern),
                    cb.like(cb.lower(root.<String>get("email")), pattern)));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        Pageable pageable = PageUtil.of(page, size, sort, dir, SORT_FIELDS, "name", Sort.Direction.ASC);
        Page<SupplierResponse> result = supplierRepository.findAll(spec, pageable).map(SupplierMapper::toResponse);
        return PageResponse.of(result);
    }

    @Override
    public SupplierResponse get(Long id) {
        return SupplierMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        if (supplierRepository.existsByNameIgnoreCase(request.getName().trim())) {
            throw new BusinessException("A supplier named '" + request.getName().trim() + "' already exists");
        }
        Supplier saved = supplierRepository.save(SupplierMapper.toEntity(request));
        log.info("Created supplier {} ({})", saved.getId(), saved.getName());
        return SupplierMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public SupplierResponse update(Long id, SupplierRequest request) {
        Supplier supplier = findOrThrow(id);
        if (supplierRepository.existsByNameIgnoreCaseAndIdNot(request.getName().trim(), id)) {
            throw new BusinessException("A supplier named '" + request.getName().trim() + "' already exists");
        }
        SupplierMapper.apply(supplier, request);
        log.info("Updated supplier {}", id);
        return SupplierMapper.toResponse(supplier);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Supplier supplier = findOrThrow(id);
        if (purchaseOrderRepository.existsBySupplierId(id)) {
            throw new BusinessException("Supplier '" + supplier.getName()
                    + "' has purchase orders and cannot be deleted; set it to INACTIVE instead");
        }
        supplierRepository.delete(supplier);
        log.info("Deleted supplier {}", id);
    }

    @Override
    @Transactional
    public SupplierResponse updateStatus(Long id, SupplierStatus status) {
        Supplier supplier = findOrThrow(id);
        supplier.setStatus(status);
        log.info("Supplier {} status -> {}", id, status);
        return SupplierMapper.toResponse(supplier);
    }

    private Supplier findOrThrow(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id " + id));
    }
}

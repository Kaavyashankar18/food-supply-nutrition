package com.foodsupply.distribution.service;

import com.foodsupply.distribution.dto.CentreRequest;
import com.foodsupply.distribution.dto.CentreResponse;
import com.foodsupply.distribution.dto.PageResponse;
import com.foodsupply.distribution.entity.CentreStatus;
import com.foodsupply.distribution.entity.DistributionCentre;
import com.foodsupply.distribution.exception.BusinessException;
import com.foodsupply.distribution.exception.ResourceNotFoundException;
import com.foodsupply.distribution.mapper.PlanMapper;
import com.foodsupply.distribution.repository.CentreRepository;
import com.foodsupply.distribution.repository.PlanRepository;
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
public class CentreServiceImpl implements CentreService {

    private static final Set<String> SORT_FIELDS = Set.of("id", "name", "status");

    private final CentreRepository centreRepository;
    private final PlanRepository planRepository;

    @Override
    public PageResponse<CentreResponse> list(String q, CentreStatus status, int page, int size, String sort, String dir) {
        Specification<DistributionCentre> spec = (root, query, cb) -> cb.conjunction();
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), like));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        String field = SORT_FIELDS.contains(sort) ? sort : "id";
        Sort.Direction direction = "desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 1000), Sort.by(direction, field));
        return PageResponse.of(centreRepository.findAll(spec, pageable).map(PlanMapper::toCentre));
    }

    @Override
    public CentreResponse get(Long id) {
        return PlanMapper.toCentre(find(id));
    }

    @Override
    @Transactional
    public CentreResponse create(CentreRequest r) {
        if (centreRepository.existsByNameIgnoreCase(r.getName().trim())) {
            throw new BusinessException("A distribution centre named '" + r.getName().trim() + "' already exists");
        }
        DistributionCentre c = DistributionCentre.builder().name(r.getName().trim()).address(r.getAddress())
                .contactPerson(r.getContactPerson()).phone(r.getPhone())
                .status(r.getStatus() != null ? r.getStatus() : CentreStatus.ACTIVE).build();
        return PlanMapper.toCentre(centreRepository.save(c));
    }

    @Override
    @Transactional
    public CentreResponse update(Long id, CentreRequest r) {
        DistributionCentre c = find(id);
        if (centreRepository.existsByNameIgnoreCaseAndIdNot(r.getName().trim(), id)) {
            throw new BusinessException("A distribution centre named '" + r.getName().trim() + "' already exists");
        }
        c.setName(r.getName().trim());
        c.setAddress(r.getAddress());
        c.setContactPerson(r.getContactPerson());
        c.setPhone(r.getPhone());
        if (r.getStatus() != null) {
            c.setStatus(r.getStatus());
        }
        return PlanMapper.toCentre(centreRepository.save(c));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        DistributionCentre c = find(id);
        if (planRepository.countByCentreId(id) > 0) {
            throw new BusinessException("Centre has distribution plans; set it INACTIVE instead of deleting");
        }
        centreRepository.delete(c);
    }

    private DistributionCentre find(Long id) {
        return centreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Distribution centre not found: " + id));
    }
}

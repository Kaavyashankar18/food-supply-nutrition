package com.foodsupply.beneficiary.service;

import com.foodsupply.beneficiary.dto.BeneficiaryDto;
import com.foodsupply.beneficiary.dto.CategorySummary;
import com.foodsupply.beneficiary.dto.EligibilityResponse;
import com.foodsupply.beneficiary.dto.NutritionRequirementDto;
import com.foodsupply.beneficiary.dto.PageResponse;
import com.foodsupply.beneficiary.entity.Beneficiary;
import com.foodsupply.beneficiary.entity.BeneficiaryCategory;
import com.foodsupply.beneficiary.entity.BeneficiaryStatus;
import com.foodsupply.beneficiary.entity.BeneficiaryType;
import com.foodsupply.beneficiary.exception.InvalidRequestException;
import com.foodsupply.beneficiary.exception.ResourceNotFoundException;
import com.foodsupply.beneficiary.mapper.BeneficiaryMapper;
import com.foodsupply.beneficiary.repository.BeneficiaryRepository;
import com.foodsupply.beneficiary.repository.NutritionRequirementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BeneficiaryServiceImpl implements BeneficiaryService {

    private final BeneficiaryRepository repository;
    private final NutritionRequirementRepository requirementRepository;
    private final EligibilityRules eligibilityRules;

    @Override
    public PageResponse<BeneficiaryDto> search(String q, BeneficiaryCategory category, BeneficiaryType type,
                                               BeneficiaryStatus status, Boolean eligible, Pageable pageable) {
        String query = (q == null || q.isBlank()) ? null : q.trim();
        return PageResponse.of(repository.search(query, category, type, status, eligible, pageable)
                .map(BeneficiaryMapper::toDto));
    }

    @Override
    public BeneficiaryDto get(Long id) {
        return BeneficiaryMapper.toDto(find(id));
    }

    @Override
    @Transactional
    public BeneficiaryDto create(BeneficiaryDto dto) {
        validate(dto);
        Beneficiary b = new Beneficiary();
        BeneficiaryMapper.copyEditable(dto, b);
        applyEligibility(b, true);
        Beneficiary saved = repository.save(b);
        log.info("Created beneficiary id={} type={} people={} eligible={}",
                saved.getId(), saved.getType(), saved.getPeopleServed(), saved.isEligible());
        return BeneficiaryMapper.toDto(saved);
    }

    @Override
    @Transactional
    public BeneficiaryDto update(Long id, BeneficiaryDto dto) {
        validate(dto);
        Beneficiary b = find(id);
        BeneficiaryMapper.copyEditable(dto, b);
        applyEligibility(b, true);
        return BeneficiaryMapper.toDto(repository.save(b));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        repository.delete(find(id));
        log.info("Deleted beneficiary id={}", id);
    }

    @Override
    public EligibilityResponse checkEligibility(Long id) {
        Beneficiary b = find(id);
        EligibilityRules.Result r = eligibilityRules.evaluate(b);
        return new EligibilityResponse(b.getId(), b.getFullName(), b.getCategory(), r.eligible(), r.reason());
    }

    @Override
    @Transactional
    public EligibilityResponse verifyEligibility(Long id) {
        Beneficiary b = find(id);
        applyEligibility(b, true);
        repository.save(b);
        return new EligibilityResponse(b.getId(), b.getFullName(), b.getCategory(), b.isEligible(), b.getEligibilityNotes());
    }

    @Override
    @Transactional
    public BeneficiaryDto changeStatus(Long id, BeneficiaryStatus status) {
        Beneficiary b = find(id);
        b.setStatus(status);
        applyEligibility(b, true);
        log.info("Beneficiary id={} set to {}", id, status);
        return BeneficiaryMapper.toDto(repository.save(b));
    }

    @Override
    public List<BeneficiaryDto> getByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return repository.findByIdIn(ids).stream().map(BeneficiaryMapper::toDto).toList();
    }

    @Override
    public List<CategorySummary> categorySummary() {
        return repository.countPeopleByCategory().stream()
                .map(row -> {
                    BeneficiaryCategory category = (BeneficiaryCategory) row[0];
                    long people = ((Number) row[1]).longValue();
                    NutritionRequirementDto req = requirementRepository.findByCategory(category)
                            .map(BeneficiaryMapper::toDto).orElse(null);
                    return new CategorySummary(category, people, req);
                })
                .sorted((a, b) -> a.getCategory().compareTo(b.getCategory()))
                .toList();
    }

    @Override
    public Map<String, Long> stats() {
        Map<String, Long> m = new LinkedHashMap<>();
        m.put("total", repository.count());
        m.put("active", repository.countByStatus(BeneficiaryStatus.ACTIVE));
        m.put("inactive", repository.countByStatus(BeneficiaryStatus.INACTIVE));
        m.put("eligibleActive", repository.countByEligibleTrueAndStatus(BeneficiaryStatus.ACTIVE));
        m.put("peopleServed", repository.totalPeopleServed());
        return m;
    }

    private Beneficiary find(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Beneficiary", id));
    }

    /** Cross-field rules that annotations cannot express. */
    private void validate(BeneficiaryDto dto) {
        BeneficiaryType type = dto.getType() == null ? BeneficiaryType.INDIVIDUAL : dto.getType();
        if (type == BeneficiaryType.INDIVIDUAL) {
            if (dto.getAge() == null) {
                throw new InvalidRequestException("age: Age is required for an individual");
            }
            if (dto.getGender() == null || dto.getGender().isBlank()) {
                throw new InvalidRequestException("gender: Gender is required for an individual");
            }
        } else if (dto.getPeopleServed() == null || dto.getPeopleServed() < 1) {
            throw new InvalidRequestException("peopleServed: A " + type + " must serve at least 1 person");
        }
    }

    private void applyEligibility(Beneficiary b, boolean stampVerification) {
        EligibilityRules.Result r = eligibilityRules.evaluate(b);
        b.setEligible(r.eligible());
        b.setEligibilityNotes(r.reason());
        if (stampVerification) {
            b.setEligibilityVerifiedOn(LocalDate.now());
        }
    }
}

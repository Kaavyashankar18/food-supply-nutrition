package com.foodsupply.beneficiary.service;

import com.foodsupply.beneficiary.dto.NutritionRequirementDto;
import com.foodsupply.beneficiary.entity.BeneficiaryCategory;
import com.foodsupply.beneficiary.entity.NutritionRequirement;
import com.foodsupply.beneficiary.exception.BusinessException;
import com.foodsupply.beneficiary.exception.ResourceNotFoundException;
import com.foodsupply.beneficiary.mapper.BeneficiaryMapper;
import com.foodsupply.beneficiary.repository.NutritionRequirementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NutritionRequirementService {

    private final NutritionRequirementRepository repository;

    public List<NutritionRequirementDto> list() {
        return repository.findAll().stream()
                .sorted((a, b) -> a.getCategory().compareTo(b.getCategory()))
                .map(BeneficiaryMapper::toDto).toList();
    }

    public NutritionRequirementDto get(Long id) {
        return BeneficiaryMapper.toDto(find(id));
    }

    public NutritionRequirementDto getByCategory(BeneficiaryCategory category) {
        return repository.findByCategory(category).map(BeneficiaryMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("No nutrition requirement defined for category " + category));
    }

    @Transactional
    public NutritionRequirementDto create(NutritionRequirementDto dto) {
        if (repository.existsByCategory(dto.getCategory())) {
            throw new BusinessException("A requirement already exists for category " + dto.getCategory());
        }
        NutritionRequirement r = new NutritionRequirement();
        BeneficiaryMapper.copyEditable(dto, r);
        return BeneficiaryMapper.toDto(repository.save(r));
    }

    @Transactional
    public NutritionRequirementDto update(Long id, NutritionRequirementDto dto) {
        NutritionRequirement r = find(id);
        if (r.getCategory() != dto.getCategory() && repository.existsByCategory(dto.getCategory())) {
            throw new BusinessException("A requirement already exists for category " + dto.getCategory());
        }
        BeneficiaryMapper.copyEditable(dto, r);
        return BeneficiaryMapper.toDto(repository.save(r));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(find(id));
    }

    private NutritionRequirement find(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Nutrition requirement", id));
    }
}

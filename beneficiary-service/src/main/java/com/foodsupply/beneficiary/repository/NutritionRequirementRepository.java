package com.foodsupply.beneficiary.repository;

import com.foodsupply.beneficiary.entity.BeneficiaryCategory;
import com.foodsupply.beneficiary.entity.NutritionRequirement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NutritionRequirementRepository extends JpaRepository<NutritionRequirement, Long> {
    Optional<NutritionRequirement> findByCategory(BeneficiaryCategory category);

    boolean existsByCategory(BeneficiaryCategory category);
}

package com.foodsupply.programme.repository;

import com.foodsupply.programme.entity.NutritionProgramme;
import com.foodsupply.programme.entity.ProgrammeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProgrammeRepository extends JpaRepository<NutritionProgramme, Long>, JpaSpecificationExecutor<NutritionProgramme> {
    long countByStatus(ProgrammeStatus status);
}

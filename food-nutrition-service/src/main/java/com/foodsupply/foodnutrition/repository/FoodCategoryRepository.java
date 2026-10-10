package com.foodsupply.foodnutrition.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.foodsupply.foodnutrition.entity.FoodCategory;

public interface FoodCategoryRepository
        extends JpaRepository<FoodCategory, Long>, JpaSpecificationExecutor<FoodCategory> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}

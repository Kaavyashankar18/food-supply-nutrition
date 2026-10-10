package com.foodsupply.foodnutrition.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.foodsupply.foodnutrition.entity.FoodItem;

public interface FoodItemRepository extends JpaRepository<FoodItem, Long>, JpaSpecificationExecutor<FoodItem> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    long countByCategoryId(Long categoryId);

    long countByActive(boolean active);

    @Query("select c.name, count(i) from FoodItem i join i.category c group by c.name order by c.name")
    List<Object[]> countItemsPerCategory();
}

package com.foodsupply.programme.repository;

import com.foodsupply.programme.entity.MealPlan;
import com.foodsupply.programme.entity.MealType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {

    List<MealPlan> findByProgrammeId(Long programmeId);

    Optional<MealPlan> findByIdAndProgrammeId(Long id, Long programmeId);

    boolean existsByProgrammeIdAndDayOfWeekAndMealType(Long programmeId, DayOfWeek dayOfWeek, MealType mealType);
}

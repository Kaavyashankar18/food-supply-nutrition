package com.foodsupply.programme.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.foodsupply.programme.client.FoodClient;
import com.foodsupply.programme.dto.MealItemRequest;
import com.foodsupply.programme.dto.MealPlanRequest;
import com.foodsupply.programme.dto.MealPlanResponse;
import com.foodsupply.programme.entity.FoodUnit;
import com.foodsupply.programme.entity.MealPlan;
import com.foodsupply.programme.entity.MealPlanItem;
import com.foodsupply.programme.entity.NutritionProgramme;
import com.foodsupply.programme.entity.ProgrammeStatus;
import com.foodsupply.programme.exception.BusinessException;
import com.foodsupply.programme.exception.InvalidRequestException;
import com.foodsupply.programme.exception.ResourceNotFoundException;
import com.foodsupply.programme.mapper.ProgrammeMapper;
import com.foodsupply.programme.repository.MealPlanRepository;
import com.foodsupply.programme.repository.ProgrammeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MealPlanServiceImpl implements MealPlanService {

    private final ProgrammeRepository programmeRepository;
    private final MealPlanRepository mealPlanRepository;
    private final FoodClient foodClient;

    @Override
    public List<MealPlanResponse> list(Long programmeId) {
        findProgramme(programmeId);
        return mealPlanRepository.findByProgrammeId(programmeId).stream()
                .sorted(Comparator.comparing((MealPlan m) -> m.getDayOfWeek().getValue())
                        .thenComparing((MealPlan m) -> m.getMealType().ordinal()))
                .map(ProgrammeMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public MealPlanResponse create(Long programmeId, MealPlanRequest r) {
        NutritionProgramme p = findProgramme(programmeId);
        ensureEditable(p);
        if (mealPlanRepository.existsByProgrammeIdAndDayOfWeekAndMealType(programmeId, r.getDayOfWeek(), r.getMealType())) {
            throw new BusinessException("A " + r.getMealType() + " plan already exists for " + r.getDayOfWeek());
        }
        MealPlan mp = MealPlan.builder().programme(p).dayOfWeek(r.getDayOfWeek()).mealType(r.getMealType()).build();
        applyItems(mp, r.getItems());
        MealPlan saved = mealPlanRepository.save(mp);
        log.info("Created meal plan {} for programme {}", saved.getId(), programmeId);
        return ProgrammeMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public MealPlanResponse update(Long programmeId, Long mealPlanId, MealPlanRequest r) {
        NutritionProgramme p = findProgramme(programmeId);
        ensureEditable(p);
        MealPlan mp = mealPlanRepository.findByIdAndProgrammeId(mealPlanId, programmeId)
                .orElseThrow(() -> new ResourceNotFoundException("Meal plan not found: " + mealPlanId));
        boolean changed = mp.getDayOfWeek() != r.getDayOfWeek() || mp.getMealType() != r.getMealType();
        if (changed && mealPlanRepository.existsByProgrammeIdAndDayOfWeekAndMealType(programmeId, r.getDayOfWeek(), r.getMealType())) {
            throw new BusinessException("A " + r.getMealType() + " plan already exists for " + r.getDayOfWeek());
        }
        mp.setDayOfWeek(r.getDayOfWeek());
        mp.setMealType(r.getMealType());
        applyItems(mp, r.getItems());
        return ProgrammeMapper.toResponse(mealPlanRepository.save(mp));
    }

    @Override
    @Transactional
    public void delete(Long programmeId, Long mealPlanId) {
        MealPlan mp = mealPlanRepository.findByIdAndProgrammeId(mealPlanId, programmeId)
                .orElseThrow(() -> new ResourceNotFoundException("Meal plan not found: " + mealPlanId));
        mealPlanRepository.delete(mp);
    }

    private void applyItems(MealPlan mp, List<MealItemRequest> requests) {
        Set<Long> ids = new LinkedHashSet<>();
        for (MealItemRequest i : requests) {
            if (!ids.add(i.getFoodItemId())) {
                throw new InvalidRequestException("Duplicate food item in meal plan: " + i.getFoodItemId());
            }
        }
        Map<Long, JsonNode> foods = new HashMap<>();
        for (JsonNode n : foodClient.byIds(ids)) {
            foods.put(n.path("id").asLong(), n);
        }
        mp.getItems().clear();
        for (MealItemRequest i : requests) {
            JsonNode f = foods.get(i.getFoodItemId());
            if (f == null) {
                throw new InvalidRequestException("Food item not found: " + i.getFoodItemId());
            }
            if (!f.path("active").asBoolean(true)) {
                throw new InvalidRequestException("Food item is inactive: " + f.path("name").asText());
            }
            mp.getItems().add(MealPlanItem.builder().mealPlan(mp).foodItemId(i.getFoodItemId())
                    .foodItemName(f.path("name").asText()).unit(FoodUnit.valueOf(f.path("unit").asText()))
                    .quantityPerPerson(i.getQuantityPerPerson()).build());
        }
    }

    private NutritionProgramme findProgramme(Long id) {
        return programmeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Programme not found: " + id));
    }

    private void ensureEditable(NutritionProgramme p) {
        if (p.getStatus() == ProgrammeStatus.COMPLETED || p.getStatus() == ProgrammeStatus.CANCELLED) {
            throw new BusinessException("Programme is " + p.getStatus() + "; meal plans cannot be changed");
        }
    }
}

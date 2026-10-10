package com.foodsupply.foodnutrition.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.foodsupply.foodnutrition.dto.MealCalculationRequest;
import com.foodsupply.foodnutrition.dto.MealCalculationResponse;
import com.foodsupply.foodnutrition.dto.MealItemInput;
import com.foodsupply.foodnutrition.dto.MealItemResult;
import com.foodsupply.foodnutrition.dto.NutritionTotals;
import com.foodsupply.foodnutrition.entity.FoodItem;
import com.foodsupply.foodnutrition.exception.ResourceNotFoundException;
import com.foodsupply.foodnutrition.repository.FoodItemRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MealCalculatorServiceImpl implements MealCalculatorService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final FoodItemRepository itemRepository;

    @Override
    public MealCalculationResponse calculate(MealCalculationRequest request) {
        List<Long> ids = request.getItems().stream().map(MealItemInput::getFoodItemId).distinct().toList();
        Map<Long, FoodItem> byId = new HashMap<>();
        for (FoodItem item : itemRepository.findAllById(ids)) {
            byId.put(item.getId(), item);
        }
        for (Long id : ids) {
            if (!byId.containsKey(id)) {
                throw new ResourceNotFoundException("Food item not found: " + id);
            }
        }

        BigDecimal persons = BigDecimal.valueOf(request.getPersons());
        BigDecimal days = BigDecimal.valueOf(request.getDays());

        List<MealItemResult> results = new ArrayList<>();
        BigDecimal cal = BigDecimal.ZERO;
        BigDecimal pro = BigDecimal.ZERO;
        BigDecimal carb = BigDecimal.ZERO;
        BigDecimal fat = BigDecimal.ZERO;
        BigDecimal fib = BigDecimal.ZERO;

        for (MealItemInput in : request.getItems()) {
            FoodItem item = byId.get(in.getFoodItemId());
            BigDecimal qpp = in.getQuantityPerPerson();
            BigDecimal total = qpp.multiply(persons).multiply(days).setScale(3, RoundingMode.HALF_UP);

            // grams eaten per person per day, expressed as multiples of 100 g
            BigDecimal factor = qpp.multiply(item.getGramsPerUnit()).divide(HUNDRED, 6, RoundingMode.HALF_UP);
            BigDecimal iCal = factor.multiply(item.getCaloriesPer100g());
            BigDecimal iPro = factor.multiply(item.getProteinPer100g());
            BigDecimal iCarb = factor.multiply(item.getCarbsPer100g());
            BigDecimal iFat = factor.multiply(item.getFatPer100g());
            BigDecimal iFib = factor.multiply(item.getFibrePer100g());

            cal = cal.add(iCal);
            pro = pro.add(iPro);
            carb = carb.add(iCarb);
            fat = fat.add(iFat);
            fib = fib.add(iFib);

            results.add(new MealItemResult(item.getId(), item.getName(), item.getUnit(),
                    qpp.setScale(3, RoundingMode.HALF_UP), total, round(iCal, iPro, iCarb, iFat, iFib)));
        }
        log.debug("Meal calculation: {} persons x {} days, {} items", request.getPersons(), request.getDays(),
                results.size());
        return new MealCalculationResponse(request.getPersons(), request.getDays(), results,
                round(cal, pro, carb, fat, fib));
    }

    private static NutritionTotals round(BigDecimal cal, BigDecimal pro, BigDecimal carb, BigDecimal fat,
            BigDecimal fib) {
        return new NutritionTotals(cal.setScale(2, RoundingMode.HALF_UP), pro.setScale(2, RoundingMode.HALF_UP),
                carb.setScale(2, RoundingMode.HALF_UP), fat.setScale(2, RoundingMode.HALF_UP),
                fib.setScale(2, RoundingMode.HALF_UP));
    }
}

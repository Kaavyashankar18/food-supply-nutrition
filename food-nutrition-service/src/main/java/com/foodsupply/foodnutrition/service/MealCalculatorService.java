package com.foodsupply.foodnutrition.service;

import com.foodsupply.foodnutrition.dto.MealCalculationRequest;
import com.foodsupply.foodnutrition.dto.MealCalculationResponse;

public interface MealCalculatorService {

    MealCalculationResponse calculate(MealCalculationRequest request);
}

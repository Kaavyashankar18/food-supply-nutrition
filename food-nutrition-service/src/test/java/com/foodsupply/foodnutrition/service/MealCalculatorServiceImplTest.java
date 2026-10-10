package com.foodsupply.foodnutrition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.foodsupply.foodnutrition.dto.MealCalculationRequest;
import com.foodsupply.foodnutrition.dto.MealCalculationResponse;
import com.foodsupply.foodnutrition.dto.MealItemInput;
import com.foodsupply.foodnutrition.entity.FoodItem;
import com.foodsupply.foodnutrition.entity.FoodUnit;
import com.foodsupply.foodnutrition.exception.ResourceNotFoundException;
import com.foodsupply.foodnutrition.repository.FoodItemRepository;

@ExtendWith(MockitoExtension.class)
class MealCalculatorServiceImplTest {

    @Mock
    private FoodItemRepository itemRepository;

    @InjectMocks
    private MealCalculatorServiceImpl service;

    private FoodItem rice() {
        return FoodItem.builder().id(1L).name("Rice (Raw)").unit(FoodUnit.KG)
                .gramsPerUnit(new BigDecimal("1000")).caloriesPer100g(new BigDecimal("345"))
                .proteinPer100g(new BigDecimal("6.8")).carbsPer100g(new BigDecimal("78"))
                .fatPer100g(new BigDecimal("0.6")).fibrePer100g(new BigDecimal("1.3"))
                .recommendedQtyPerPersonPerDay(new BigDecimal("0.150")).active(true).build();
    }

    @Test
    void calculatesTotalQuantityAndNutrition() {
        when(itemRepository.findAllById(anyList())).thenReturn(List.of(rice()));
        MealCalculationRequest req = new MealCalculationRequest(500, 7,
                List.of(new MealItemInput(1L, new BigDecimal("0.150"))));

        MealCalculationResponse res = service.calculate(req);

        // 0.150 kg * 500 persons * 7 days = 525 kg
        assertEquals(0, new BigDecimal("525.000").compareTo(res.items().get(0).totalQuantity()));
        // 150 g * 345 kcal / 100 g = 517.5 kcal per person per day
        assertEquals(0, new BigDecimal("517.50").compareTo(res.nutritionPerPersonPerDay().calories()));
        assertEquals(0, new BigDecimal("10.20").compareTo(res.nutritionPerPersonPerDay().protein()));
    }

    @Test
    void unknownFoodItemThrowsNotFound() {
        when(itemRepository.findAllById(anyList())).thenReturn(List.of());
        MealCalculationRequest req = new MealCalculationRequest(10, 1,
                List.of(new MealItemInput(99L, new BigDecimal("0.100"))));

        assertThrows(ResourceNotFoundException.class, () -> service.calculate(req));
    }
}

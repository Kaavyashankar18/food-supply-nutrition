package com.foodsupply.foodnutrition.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.foodsupply.foodnutrition.dto.FoodItemRequest;
import com.foodsupply.foodnutrition.entity.FoodItem;
import com.foodsupply.foodnutrition.entity.FoodUnit;
import com.foodsupply.foodnutrition.exception.BusinessException;
import com.foodsupply.foodnutrition.repository.FoodCategoryRepository;
import com.foodsupply.foodnutrition.repository.FoodItemRepository;

@ExtendWith(MockitoExtension.class)
class FoodItemServiceImplTest {

    @Mock
    private FoodItemRepository itemRepository;

    @Mock
    private FoodCategoryRepository categoryRepository;

    @InjectMocks
    private FoodItemServiceImpl service;

    @Test
    void duplicateNameIsRejectedWithBusinessException() {
        FoodItemRequest req = FoodItemRequest.builder().name("Rice (Raw)").categoryId(1L).unit(FoodUnit.KG)
                .gramsPerUnit(new BigDecimal("1000")).caloriesPer100g(new BigDecimal("345"))
                .proteinPer100g(new BigDecimal("6.8")).carbsPer100g(new BigDecimal("78"))
                .fatPer100g(new BigDecimal("0.6")).fibrePer100g(new BigDecimal("1.3"))
                .recommendedQtyPerPersonPerDay(new BigDecimal("0.150")).build();
        when(itemRepository.existsByNameIgnoreCase("Rice (Raw)")).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.create(req));
        verify(itemRepository, never()).save(org.mockito.ArgumentMatchers.any(FoodItem.class));
    }
}

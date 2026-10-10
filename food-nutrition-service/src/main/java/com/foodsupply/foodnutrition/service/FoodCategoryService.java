package com.foodsupply.foodnutrition.service;

import java.util.List;

import com.foodsupply.foodnutrition.dto.FoodCategoryRequest;
import com.foodsupply.foodnutrition.dto.FoodCategoryResponse;
import com.foodsupply.foodnutrition.dto.PageResponse;

public interface FoodCategoryService {

    PageResponse<FoodCategoryResponse> search(String q, int page, int size, String sort, String dir);

    List<FoodCategoryResponse> listAll();

    FoodCategoryResponse get(Long id);

    FoodCategoryResponse create(FoodCategoryRequest request);

    FoodCategoryResponse update(Long id, FoodCategoryRequest request);

    void delete(Long id);
}

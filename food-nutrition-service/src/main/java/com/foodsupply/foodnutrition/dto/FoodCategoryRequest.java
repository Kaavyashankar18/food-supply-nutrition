package com.foodsupply.foodnutrition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodCategoryRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 80, message = "Name must be at most 80 characters")
    private String name;
}

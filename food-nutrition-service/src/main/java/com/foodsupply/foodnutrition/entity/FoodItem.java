package com.foodsupply.foodnutrition.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "food_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private FoodCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FoodUnit unit;

    /** Weight in grams of ONE unit (1000 for kg/litre, 50 for one egg). */
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal gramsPerUnit;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal caloriesPer100g;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal proteinPer100g;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal carbsPer100g;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal fatPer100g;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal fibrePer100g;

    @Column(length = 255)
    private String allergens;

    @Column(length = 255)
    private String storageRequirement;

    /** In the item's own unit (kg / litre / piece). */
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal recommendedQtyPerPersonPerDay;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}

package com.foodsupply.programme.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "meal_plan_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MealPlanItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meal_plan_id", nullable = false)
    private MealPlan mealPlan;

    @Column(nullable = false)
    private Long foodItemId;

    @Column(nullable = false, length = 150)
    private String foodItemName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FoodUnit unit;

    /** Quantity per person per day, in the item's unit. */
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantityPerPerson;
}

package com.foodsupply.programme.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "meal_plans",
        uniqueConstraints = @UniqueConstraint(columnNames = {"programme_id", "dayOfWeek", "mealType"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MealPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "programme_id", nullable = false)
    private NutritionProgramme programme;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private DayOfWeek dayOfWeek;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MealType mealType;

    @OneToMany(mappedBy = "mealPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MealPlanItem> items = new ArrayList<>();
}

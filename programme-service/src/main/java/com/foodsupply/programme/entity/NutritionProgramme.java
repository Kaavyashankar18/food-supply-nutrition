package com.foodsupply.programme.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "nutrition_programmes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NutritionProgramme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 60)
    private String type;

    @Column(length = 1000)
    private String objectives;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private BeneficiaryCategory targetCategory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ProgrammeStatus status = ProgrammeStatus.DRAFT;

    @OneToMany(mappedBy = "programme", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Participation> participations = new ArrayList<>();

    @OneToMany(mappedBy = "programme", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MealPlan> mealPlans = new ArrayList<>();
}

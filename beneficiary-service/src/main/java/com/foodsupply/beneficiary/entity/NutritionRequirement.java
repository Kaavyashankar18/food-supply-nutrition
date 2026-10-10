package com.foodsupply.beneficiary.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Daily nutrition requirement for one beneficiary category. */
@Entity
@Table(name = "nutrition_requirements", uniqueConstraints =
        @UniqueConstraint(name = "uk_requirement_category", columnNames = "category"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NutritionRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BeneficiaryCategory category;

    @Column(nullable = false)
    private Integer dailyCalories;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal dailyProteinG;

    @Column(precision = 8, scale = 2)
    private BigDecimal dailyCarbsG;

    @Column(precision = 8, scale = 2)
    private BigDecimal dailyFatG;

    @Column(precision = 8, scale = 2)
    private BigDecimal dailyFibreG;

    @Column(length = 255)
    private String notes;
}

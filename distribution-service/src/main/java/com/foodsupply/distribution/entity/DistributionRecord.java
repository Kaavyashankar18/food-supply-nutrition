package com.foodsupply.distribution.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "distribution_records")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DistributionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private DistributionPlan plan;

    @Column(nullable = false)
    private Long beneficiaryId;

    @Column(nullable = false, length = 150)
    private String beneficiaryName;

    @Column(nullable = false)
    private Long foodItemId;

    @Column(nullable = false, length = 150)
    private String foodItemName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FoodUnit unit;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(nullable = false)
    private LocalDate deliveredOn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RecordStatus status;
}

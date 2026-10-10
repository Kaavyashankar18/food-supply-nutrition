package com.foodsupply.distribution.entity;

import jakarta.persistence.*;
import lombok.*;

/** Snapshot of an enrolled programme participant at plan creation (referenced by id only). */
@Entity
@Table(name = "plan_beneficiaries")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlanBeneficiary {

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
    private int peopleCovered;
}

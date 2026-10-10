package com.foodsupply.distribution.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "distribution_plans")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DistributionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 30)
    private String planNumber;

    /** Reference to programme-service (by id only). */
    @Column(nullable = false)
    private Long programmeId;

    @Column(nullable = false, length = 150)
    private String programmeName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "centre_id", nullable = false)
    private DistributionCentre centre;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private PlanStatus status = PlanStatus.PLANNED;

    @Column(length = 500)
    private String remarks;

    @Column(nullable = false)
    private LocalDate createdOn;

    private LocalDate dispatchedOn;

    private LocalDate deliveredOn;

    @Column(nullable = false)
    private int enrolledPeople;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DistributionItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PlanBeneficiary> beneficiaries = new ArrayList<>();
}

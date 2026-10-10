package com.foodsupply.distribution.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "distribution_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DistributionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private DistributionPlan plan;

    @Column(nullable = false)
    private Long foodItemId;

    @Column(nullable = false, length = 150)
    private String foodItemName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FoodUnit unit;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal plannedQty;

    @Column(nullable = false, precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal dispatchedQty = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal deliveredQty = BigDecimal.ZERO;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DistributionAllocation> allocations = new ArrayList<>();
}

package com.foodsupply.distribution.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Batch allocation returned by inventory-service when stock was reserved (FEFO). */
@Entity
@Table(name = "distribution_allocations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DistributionAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private DistributionItem item;

    @Column(nullable = false)
    private Long batchId;

    @Column(length = 60)
    private String batchNumber;

    private LocalDate expiryDate;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;
}

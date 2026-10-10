package com.foodsupply.inventory.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stock_batches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long foodItemId;

    @Column(nullable = false, unique = true, length = 60)
    private String batchNumber;

    private Long supplierId;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantityReceived;

    /** Physically on hand (includes the reserved part). */
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantityAvailable;

    @Column(nullable = false, precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal quantityReserved = BigDecimal.ZERO;

    private LocalDate receivedDate;

    /** null = does not expire. */
    private LocalDate expiryDate;

    @Column(length = 100)
    private String storageLocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private QualityStatus qualityStatus = QualityStatus.APPROVED;
}

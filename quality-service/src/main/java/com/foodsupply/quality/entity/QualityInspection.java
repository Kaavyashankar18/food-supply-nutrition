package com.foodsupply.quality.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "quality_inspections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QualityInspection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate inspectionDate;

    /** Reference to food-nutrition-service (by id only). */
    @Column(nullable = false)
    private Long foodItemId;

    @Column(nullable = false, length = 150)
    private String foodItemName;

    /** Reference to an inventory-service batch (by id only). */
    @Column(nullable = false)
    private Long batchId;

    @Column(nullable = false, length = 60)
    private String batchNumber;

    @Column(nullable = false, length = 100)
    private String inspector;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InspectionResult result;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QualityStatus qualityStatus;

    @Column(length = 1000)
    private String remarks;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

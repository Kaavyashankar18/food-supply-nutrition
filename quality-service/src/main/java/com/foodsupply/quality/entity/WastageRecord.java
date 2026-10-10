package com.foodsupply.quality.entity;

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

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "wastage_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WastageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long batchId;

    @Column(nullable = false, length = 60)
    private String batchNumber;

    @Column(nullable = false)
    private Long foodItemId;

    @Column(nullable = false, length = 150)
    private String foodItemName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FoodUnit unit;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WastageReason reason;

    @Column(length = 150)
    private String disposalMethod;

    @Column(nullable = false)
    private LocalDate recordedOn;

    @Column(nullable = false, length = 100)
    private String recordedBy;

    @Column(length = 1000)
    private String remarks;
}

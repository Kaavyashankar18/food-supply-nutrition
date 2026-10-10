package com.foodsupply.procurement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "request_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private ProcurementRequest request;

    /** Reference to food-nutrition-service (ID only, no foreign key). */
    @Column(nullable = false)
    private Long foodItemId;

    @Column(nullable = false, length = 150)
    private String foodItemName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FoodUnit unit;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(precision = 12, scale = 2)
    private BigDecimal estimatedUnitPrice;
}

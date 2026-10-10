package com.foodsupply.inventory.entity;

import java.math.BigDecimal;

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

/** One row per food item (referenced by id only; the name/unit are a local copy). */
@Entity
@Table(name = "stock_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long foodItemId;

    @Column(nullable = false, length = 120)
    private String foodItemName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FoodUnit unit;

    @Column(nullable = false, precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal minimumStockLevel = BigDecimal.ZERO;

    @Column(length = 100)
    private String storageLocation;
}

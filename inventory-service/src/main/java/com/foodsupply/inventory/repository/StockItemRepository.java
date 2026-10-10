package com.foodsupply.inventory.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.foodsupply.inventory.entity.StockItem;

public interface StockItemRepository extends JpaRepository<StockItem, Long> {

    Optional<StockItem> findByFoodItemId(Long foodItemId);

    List<StockItem> findByFoodItemIdIn(Collection<Long> foodItemIds);
}

package com.foodsupply.inventory.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.foodsupply.inventory.entity.FoodUnit;
import com.foodsupply.inventory.entity.MovementType;
import com.foodsupply.inventory.entity.QualityStatus;
import com.foodsupply.inventory.entity.StockBatch;
import com.foodsupply.inventory.entity.StockItem;
import com.foodsupply.inventory.entity.StockMovement;
import com.foodsupply.inventory.repository.StockBatchRepository;
import com.foodsupply.inventory.repository.StockItemRepository;
import com.foodsupply.inventory.repository.StockMovementRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Seeds stock items and batches only when the tables are empty.
 * The foodItemIds (1..32) and names mirror the food-nutrition-service seed (fresh food_db, same order).
 * Dates are relative to today so the alerts always show: low stock, expiring within 7 days,
 * one expired batch and one quarantined batch.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private final StockItemRepository itemRepository;
    private final StockBatchRepository batchRepository;
    private final StockMovementRepository movementRepository;

    private final Map<Long, Integer> batchSeq = new HashMap<>();
    private final Random random = new Random(42);

    @Override
    public void run(String... args) {
        if (itemRepository.count() > 0 || batchRepository.count() > 0) {
            log.info("Inventory data already present - seeding skipped");
            return;
        }
        LocalDate today = LocalDate.now();
        String dry = "Dry Store A";
        String cold = "Cold Room 1";
        String veg = "Vegetable Shed";
        String fruit = "Fruit Store";

        List<StockItem> items = new ArrayList<>();
        // id, name, unit, minimum level, location
        items.add(item(1, "Rice (Raw)", FoodUnit.KG, 200, dry));
        items.add(item(2, "Wheat Flour (Atta)", FoodUnit.KG, 150, dry));
        items.add(item(3, "Ragi (Finger Millet)", FoodUnit.KG, 50, dry));
        items.add(item(4, "Semolina (Rava)", FoodUnit.KG, 40, dry));
        items.add(item(5, "Toor Dal", FoodUnit.KG, 100, dry));
        items.add(item(6, "Moong Dal", FoodUnit.KG, 60, dry));
        items.add(item(7, "Chickpeas (Kabuli Chana)", FoodUnit.KG, 50, dry));
        items.add(item(8, "Groundnuts", FoodUnit.KG, 30, dry));
        items.add(item(9, "Soya Chunks", FoodUnit.KG, 30, dry));
        items.add(item(10, "Potato", FoodUnit.KG, 150, veg));
        items.add(item(11, "Onion", FoodUnit.KG, 100, veg));
        items.add(item(12, "Tomato", FoodUnit.KG, 80, veg));
        items.add(item(13, "Carrot", FoodUnit.KG, 60, veg));
        items.add(item(14, "Spinach (Palak)", FoodUnit.KG, 40, veg));
        items.add(item(15, "Brinjal (Eggplant)", FoodUnit.KG, 60, veg));
        items.add(item(16, "Cabbage", FoodUnit.KG, 60, veg));
        items.add(item(17, "Banana", FoodUnit.PIECE, 500, fruit));
        items.add(item(18, "Apple", FoodUnit.PIECE, 500, fruit));
        items.add(item(19, "Orange", FoodUnit.PIECE, 500, fruit));
        items.add(item(20, "Papaya", FoodUnit.KG, 50, fruit));
        items.add(item(21, "Milk (Toned)", FoodUnit.LITRE, 300, cold));
        items.add(item(22, "Curd (Yogurt)", FoodUnit.KG, 50, cold));
        items.add(item(23, "Paneer", FoodUnit.KG, 20, cold));
        items.add(item(24, "Ghee", FoodUnit.KG, 20, dry));
        items.add(item(25, "Egg", FoodUnit.PIECE, 1000, cold));
        items.add(item(26, "Chicken (Broiler)", FoodUnit.KG, 30, cold));
        items.add(item(27, "Sunflower Oil", FoodUnit.LITRE, 60, dry));
        items.add(item(28, "Groundnut Oil", FoodUnit.LITRE, 40, dry));
        items.add(item(29, "Sugar", FoodUnit.KG, 50, dry));
        items.add(item(30, "Jaggery", FoodUnit.KG, 20, dry));
        items.add(item(31, "Iodised Salt", FoodUnit.KG, 30, dry));
        items.add(item(32, "Turmeric Powder", FoodUnit.KG, 5, dry));
        itemRepository.saveAll(items);
        Map<Long, StockItem> byFoodId = new HashMap<>();
        for (StockItem i : items) {
            byFoodId.put(i.getFoodItemId(), i);
        }

        List<StockBatch> batches = new ArrayList<>();
        // ---- Grains (healthy, except Ragi = low stock)
        batches.add(batch(byFoodId, 1, 800, 300, today, QualityStatus.APPROVED));
        batches.add(batch(byFoodId, 1, 500, 200, today, QualityStatus.APPROVED));
        batches.add(batch(byFoodId, 2, 400, 150, today, QualityStatus.APPROVED));
        batches.add(batch(byFoodId, 2, 250, 90, today, QualityStatus.APPROVED));
        batches.add(batch(byFoodId, 3, 30, 120, today, QualityStatus.APPROVED));
        batches.add(batch(byFoodId, 4, 120, 180, today, QualityStatus.APPROVED));
        // ---- Pulses (Toor Dal = low stock; one quarantined Soya batch)
        batches.add(batch(byFoodId, 5, 60, 200, today, QualityStatus.APPROVED));
        batches.add(batch(byFoodId, 5, 30, 60, today, QualityStatus.APPROVED));
        batches.add(batch(byFoodId, 6, 200, 220, today, QualityStatus.APPROVED));
        batches.add(batch(byFoodId, 7, 150, 200, today, QualityStatus.APPROVED));
        batches.add(batch(byFoodId, 8, 80, 150, today, QualityStatus.APPROVED));
        batches.add(batch(byFoodId, 9, 100, 200, today, QualityStatus.APPROVED));
        batches.add(batch(byFoodId, 9, 40, 180, today, QualityStatus.QUARANTINED));
        // ---- Vegetables (Tomato and Spinach expire soon; Spinach = low stock)
        batches.add(batch(byFoodId, 10, 400, 25, today, QualityStatus.APPROVED));
        batches.add(batch(byFoodId, 10, 200, 20, today, QualityStatus.APPROVED));
        batches.add(batch(byFoodId, 11, 300, 30, today, QualityStatus.APPROVED));
        batches.add(batch(byFoodId, 12, 100, 4, today, QualityStatus.APPROVED));
        batches.add(batch(byFoodId, 12, 120, 8, today, QualityStatus.APPROVED));
        batches.add(batch(byFoodId, 13, 150, 14, today, QualityStatus.APPROVED));
        batches.add(batch(byFoodId, 14, 25, 3, today, QualityStatus.APPROVED));
        batches.add(batch(byFoodId, 15, 120, 10, today, QualityStatus.APPROVED));
        batches.add(batch(byFoodId, 16, 130, 12, today, QualityStatus.APPROVED));
        // ---- Fruits (Banana batch expires soon)
        batches.add(batch(byFoodId, 17, 1500, 6, today, QualityStatus.APPROVED));
        batches.add(batch(byFoodId, 17, 2000, 10, today, QualityStatus.APPROVED));
        batches.add(batch(byFoodId, 18, 2500, 20, today, QualityStatus.APPROVED));
        batches.add(batch(byFoodId, 19, 1800, 15, today, QualityStatus.APPROVED));
        batches.add(batch(byFoodId, 20, 70, 12, today, QualityStatus.APPROVED));
        // ---- Dairy (Milk batch expires soon; one EXPIRED Curd batch)
        batches.add(batch(byFoodId, 21, 400, 3, today, QualityStatus.APPROVED));
        batches.add(batch(byFoodId, 21, 250, 5, today, QualityStatus.APPROVED));
        batches.add(batch(byFoodId, 22, 80, 9, today, QualityStatus.APPROVED));
        batches.add(batch(byFoodId, 22, 40, -2, today, QualityStatus.APPROVED));
        batches.add(batch(byFoodId, 23, 35, 9, today, QualityStatus.APPROVED));
        batches.add(batch(byFoodId, 24, 60, 150, today, QualityStatus.APPROVED));
        // ---- Eggs / protein (Egg = low stock: 900 < 1000; Chicken expires soon)
        batches.add(batch(byFoodId, 25, 600, 12, today, QualityStatus.APPROVED));
        batches.add(batch(byFoodId, 25, 300, 20, today, QualityStatus.APPROVED));
        batches.add(batch(byFoodId, 26, 50, 4, today, QualityStatus.APPROVED));
        // ---- Oils (Groundnut Oil = low stock)
        batches.add(batch(byFoodId, 27, 150, 200, today, QualityStatus.APPROVED));
        batches.add(batch(byFoodId, 28, 35, 180, today, QualityStatus.APPROVED));
        // ---- Others
        batches.add(batch(byFoodId, 29, 120, 300, today, QualityStatus.APPROVED));
        batches.add(batch(byFoodId, 30, 40, 200, today, QualityStatus.APPROVED));
        batches.add(batch(byFoodId, 31, 100, 500, today, QualityStatus.APPROVED));
        batches.add(batch(byFoodId, 32, 12, 300, today, QualityStatus.APPROVED));

        batchRepository.saveAll(batches);

        List<StockMovement> movements = new ArrayList<>();
        for (StockBatch b : batches) {
            LocalDateTime when = (b.getReceivedDate() != null ? b.getReceivedDate() : today).atTime(9, 0);
            movements.add(StockMovement.builder().batchId(b.getId()).foodItemId(b.getFoodItemId())
                    .type(MovementType.RECEIPT).quantity(b.getQuantityReceived()).referenceType("RECEIPT")
                    .referenceId(b.getId()).createdAt(when).remarks("Opening stock - batch " + b.getBatchNumber())
                    .build());
        }
        movementRepository.saveAll(movements);
        log.info("Seeded {} stock items and {} batches", items.size(), batches.size());
    }

    private static StockItem item(long foodItemId, String name, FoodUnit unit, double min, String location) {
        return StockItem.builder().foodItemId(foodItemId).foodItemName(name).unit(unit)
                .minimumStockLevel(BigDecimal.valueOf(min)).storageLocation(location).build();
    }

    private StockBatch batch(Map<Long, StockItem> items, long foodItemId, double qty, int expiryInDays,
            LocalDate today, QualityStatus status) {
        int seq = batchSeq.merge(foodItemId, 1, Integer::sum);
        String number = String.format("BN-%02d-%02d", foodItemId, seq);
        LocalDate received = today.minusDays(5L + random.nextInt(36));
        long supplierId = 1L + random.nextInt(4);
        BigDecimal q = BigDecimal.valueOf(qty);
        return StockBatch.builder().foodItemId(foodItemId).batchNumber(number).supplierId(supplierId)
                .quantityReceived(q).quantityAvailable(q).quantityReserved(BigDecimal.ZERO).receivedDate(received)
                .expiryDate(today.plusDays(expiryInDays)).storageLocation(items.get(foodItemId).getStorageLocation())
                .qualityStatus(status).build();
    }
}

package com.foodsupply.foodnutrition.config;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.foodsupply.foodnutrition.entity.FoodCategory;
import com.foodsupply.foodnutrition.entity.FoodItem;
import com.foodsupply.foodnutrition.entity.FoodUnit;
import com.foodsupply.foodnutrition.repository.FoodCategoryRepository;
import com.foodsupply.foodnutrition.repository.FoodItemRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Seeds categories and ~32 Indian food items, only when the tables are empty.
 * Items are saved in a fixed order, so on a fresh database the ids are stable (1..32).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private final FoodCategoryRepository categoryRepository;
    private final FoodItemRepository itemRepository;

    @Override
    public void run(String... args) {
        if (categoryRepository.count() > 0 || itemRepository.count() > 0) {
            log.info("Food data already present - seeding skipped");
            return;
        }
        Map<String, FoodCategory> cats = new LinkedHashMap<>();
        for (String n : List.of("Grains", "Pulses", "Vegetables", "Fruits", "Dairy", "Eggs/Protein", "Oils",
                "Others")) {
            cats.put(n, categoryRepository.save(FoodCategory.builder().name(n).build()));
        }

        List<FoodItem> items = new ArrayList<>();
        String dry = "Cool, dry place; airtight container";
        String cold = "Refrigerate at 2-5 C";
        String veg = "Cool, ventilated place; use within a week";

        // name, category, unit, gramsPerUnit, kcal, protein, carbs, fat, fibre, allergens, storage, recQty/person/day
        // ---- Grains
        items.add(item("Rice (Raw)", cats.get("Grains"), FoodUnit.KG, 1000, 345, 6.8, 78, 0.6, 1.3, null, dry, 0.150));
        items.add(item("Wheat Flour (Atta)", cats.get("Grains"), FoodUnit.KG, 1000, 340, 12, 72, 1.7, 11, "Gluten", dry, 0.100));
        items.add(item("Ragi (Finger Millet)", cats.get("Grains"), FoodUnit.KG, 1000, 336, 7.3, 72, 1.3, 3.6, null, dry, 0.030));
        items.add(item("Semolina (Rava)", cats.get("Grains"), FoodUnit.KG, 1000, 360, 12.7, 73, 1, 3.9, "Gluten", dry, 0.020));
        // ---- Pulses
        items.add(item("Toor Dal", cats.get("Pulses"), FoodUnit.KG, 1000, 335, 22, 63, 1.5, 15, null, dry, 0.040));
        items.add(item("Moong Dal", cats.get("Pulses"), FoodUnit.KG, 1000, 347, 24, 63, 1.2, 16, null, dry, 0.030));
        items.add(item("Chickpeas (Kabuli Chana)", cats.get("Pulses"), FoodUnit.KG, 1000, 364, 19, 61, 6, 17, null, dry, 0.030));
        items.add(item("Groundnuts", cats.get("Pulses"), FoodUnit.KG, 1000, 567, 26, 16, 49, 8.5, "Peanuts", dry, 0.020));
        items.add(item("Soya Chunks", cats.get("Pulses"), FoodUnit.KG, 1000, 345, 52, 33, 0.5, 13, "Soy", dry, 0.015));
        // ---- Vegetables
        items.add(item("Potato", cats.get("Vegetables"), FoodUnit.KG, 1000, 77, 2, 17, 0.1, 2.2, null, veg, 0.100));
        items.add(item("Onion", cats.get("Vegetables"), FoodUnit.KG, 1000, 40, 1.1, 9, 0.1, 1.7, null, veg, 0.050));
        items.add(item("Tomato", cats.get("Vegetables"), FoodUnit.KG, 1000, 18, 0.9, 3.9, 0.2, 1.2, null, veg, 0.050));
        items.add(item("Carrot", cats.get("Vegetables"), FoodUnit.KG, 1000, 41, 0.9, 10, 0.2, 2.8, null, veg, 0.050));
        items.add(item("Spinach (Palak)", cats.get("Vegetables"), FoodUnit.KG, 1000, 23, 2.9, 3.6, 0.4, 2.2, null, cold, 0.050));
        items.add(item("Brinjal (Eggplant)", cats.get("Vegetables"), FoodUnit.KG, 1000, 25, 1, 6, 0.2, 3, null, veg, 0.050));
        items.add(item("Cabbage", cats.get("Vegetables"), FoodUnit.KG, 1000, 25, 1.3, 6, 0.1, 2.5, null, veg, 0.050));
        // ---- Fruits
        items.add(item("Banana", cats.get("Fruits"), FoodUnit.PIECE, 120, 89, 1.1, 23, 0.3, 2.6, null, "Room temperature, ventilated", 1.000));
        items.add(item("Apple", cats.get("Fruits"), FoodUnit.PIECE, 150, 52, 0.3, 14, 0.2, 2.4, null, cold, 1.000));
        items.add(item("Orange", cats.get("Fruits"), FoodUnit.PIECE, 130, 47, 0.9, 12, 0.1, 2.4, null, "Cool, ventilated place", 1.000));
        items.add(item("Papaya", cats.get("Fruits"), FoodUnit.KG, 1000, 43, 0.5, 11, 0.3, 1.7, null, "Room temperature; refrigerate when ripe", 0.100));
        // ---- Dairy
        items.add(item("Milk (Toned)", cats.get("Dairy"), FoodUnit.LITRE, 1000, 61, 3.2, 4.8, 3.3, 0, "Milk (lactose)", cold, 0.200));
        items.add(item("Curd (Yogurt)", cats.get("Dairy"), FoodUnit.KG, 1000, 60, 3.1, 4.7, 3.3, 0, "Milk (lactose)", cold, 0.100));
        items.add(item("Paneer", cats.get("Dairy"), FoodUnit.KG, 1000, 265, 18, 1.2, 20, 0, "Milk (lactose)", cold, 0.030));
        items.add(item("Ghee", cats.get("Dairy"), FoodUnit.KG, 1000, 900, 0, 0, 100, 0, "Milk", dry, 0.010));
        // ---- Eggs / Protein
        items.add(item("Egg", cats.get("Eggs/Protein"), FoodUnit.PIECE, 50, 143, 12.6, 0.7, 9.5, 0, "Egg", cold, 1.000));
        items.add(item("Chicken (Broiler)", cats.get("Eggs/Protein"), FoodUnit.KG, 1000, 120, 22, 0, 3, 0, null, "Frozen at -18 C or refrigerated for 1 day", 0.050));
        // ---- Oils
        items.add(item("Sunflower Oil", cats.get("Oils"), FoodUnit.LITRE, 1000, 884, 0, 0, 100, 0, null, "Cool, dark place", 0.020));
        items.add(item("Groundnut Oil", cats.get("Oils"), FoodUnit.LITRE, 1000, 884, 0, 0, 100, 0, "Peanuts", "Cool, dark place", 0.015));
        // ---- Others
        items.add(item("Sugar", cats.get("Others"), FoodUnit.KG, 1000, 387, 0, 100, 0, 0, null, dry, 0.020));
        items.add(item("Jaggery", cats.get("Others"), FoodUnit.KG, 1000, 383, 0.4, 98, 0.1, 0, null, dry, 0.015));
        items.add(item("Iodised Salt", cats.get("Others"), FoodUnit.KG, 1000, 0, 0, 0, 0, 0, null, dry, 0.005));
        items.add(item("Turmeric Powder", cats.get("Others"), FoodUnit.KG, 1000, 312, 9.7, 67, 3.3, 22, null, dry, 0.002));

        for (FoodItem i : items) {
            itemRepository.save(i);
        }
        log.info("Seeded {} food categories and {} food items", cats.size(), items.size());
    }

    private static FoodItem item(String name, FoodCategory category, FoodUnit unit, double gramsPerUnit,
            double kcal, double protein, double carbs, double fat, double fibre, String allergens,
            String storage, double recQty) {
        return FoodItem.builder()
                .name(name)
                .category(category)
                .unit(unit)
                .gramsPerUnit(BigDecimal.valueOf(gramsPerUnit))
                .caloriesPer100g(BigDecimal.valueOf(kcal))
                .proteinPer100g(BigDecimal.valueOf(protein))
                .carbsPer100g(BigDecimal.valueOf(carbs))
                .fatPer100g(BigDecimal.valueOf(fat))
                .fibrePer100g(BigDecimal.valueOf(fibre))
                .allergens(allergens)
                .storageRequirement(storage)
                .recommendedQtyPerPersonPerDay(BigDecimal.valueOf(recQty))
                .active(true)
                .build();
    }
}

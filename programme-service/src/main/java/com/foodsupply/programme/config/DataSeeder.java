package com.foodsupply.programme.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.foodsupply.programme.client.BeneficiaryClient;
import com.foodsupply.programme.client.FoodClient;
import com.foodsupply.programme.entity.BeneficiaryCategory;
import com.foodsupply.programme.entity.FoodUnit;
import com.foodsupply.programme.entity.MealPlan;
import com.foodsupply.programme.entity.MealPlanItem;
import com.foodsupply.programme.entity.MealType;
import com.foodsupply.programme.entity.NutritionProgramme;
import com.foodsupply.programme.entity.Participation;
import com.foodsupply.programme.entity.ParticipationStatus;
import com.foodsupply.programme.entity.ProgrammeStatus;
import com.foodsupply.programme.repository.ProgrammeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Seeds the demo programme. Needs beneficiary-service (8081) and food-nutrition-service (8082) running;
 * if either is down the seed is skipped (tables stay empty, so it is retried on the next start).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    // food ids from the food-nutrition seed
    private static final long[] CORE_IDS = {1, 5, 10, 11, 12, 27, 28};
    private static final String[] CORE_QTY = {"0.100", "0.040", "0.100", "0.020", "0.030", "0.015", "0.010"};
    private static final long RICE = 1, TOOR_DAL = 5, POTATO = 10, ONION = 11, TOMATO = 12, SPINACH = 14,
            BANANA = 17, MILK = 21, EGG = 25, SUNFLOWER_OIL = 27, GROUNDNUT_OIL = 28;
    private static final List<Long> FOOD_IDS = List.of(RICE, TOOR_DAL, POTATO, ONION, TOMATO, SPINACH, BANANA, MILK,
            EGG, SUNFLOWER_OIL, GROUNDNUT_OIL);

    private final ProgrammeRepository programmeRepository;
    private final BeneficiaryClient beneficiaryClient;
    private final FoodClient foodClient;

    @Override
    public void run(String... args) {
        if (programmeRepository.count() > 0) {
            log.info("Programme data already present - seeding skipped");
            return;
        }
        JsonNode school = null;
        Map<Long, JsonNode> foods = new HashMap<>();
        try {
            JsonNode content = beneficiaryClient.search("Adyar", "SCHOOL").path("content");
            if (!content.isArray() || content.size() == 0) {
                log.warn("Programme seed skipped: school 'Adyar' not found in beneficiary-service");
                return;
            }
            school = content.get(0);
            for (JsonNode n : foodClient.byIds(FOOD_IDS)) {
                foods.put(n.path("id").asLong(), n);
            }
        } catch (RuntimeException e) {
            log.warn("Programme seed skipped (start beneficiary-service and food-nutrition-service first): {}", e.getMessage());
            return;
        }

        LocalDate today = LocalDate.now();
        NutritionProgramme main = NutritionProgramme.builder()
                .name("School Mid-Day Meal Programme").type("MID_DAY_MEAL")
                .objectives("Provide a nutritious cooked mid-day meal to school students for the week.")
                .startDate(today).endDate(today.plusDays(6)).targetCategory(BeneficiaryCategory.STUDENT)
                .status(ProgrammeStatus.ACTIVE).build();
        int people = Math.max(1, school.path("peopleServed").asInt(1));
        main.getParticipations().add(Participation.builder().programme(main)
                .beneficiaryId(school.path("id").asLong()).beneficiaryName(school.path("fullName").asText())
                .peopleCovered(people).enrolledOn(today).status(ParticipationStatus.ENROLLED).build());

        Map<DayOfWeek, Map<Long, String>> extras = Map.of(
                DayOfWeek.MONDAY, Map.of(EGG, "1", MILK, "0.200"),
                DayOfWeek.TUESDAY, Map.of(SPINACH, "0.050", BANANA, "1"),
                DayOfWeek.WEDNESDAY, Map.of(EGG, "1", MILK, "0.200"),
                DayOfWeek.THURSDAY, Map.of(SPINACH, "0.050", BANANA, "1", MILK, "0.200"),
                DayOfWeek.FRIDAY, Map.of(EGG, "1", BANANA, "1", MILK, "0.200"),
                DayOfWeek.SATURDAY, Map.of(BANANA, "1"),
                DayOfWeek.SUNDAY, Map.of(BANANA, "1"));
        for (DayOfWeek d : DayOfWeek.values()) {
            MealPlan mp = MealPlan.builder().programme(main).dayOfWeek(d).mealType(MealType.MID_DAY_MEAL).build();
            for (int i = 0; i < CORE_IDS.length; i++) {
                addItem(mp, foods.get(CORE_IDS[i]), CORE_QTY[i]);
            }
            for (Map.Entry<Long, String> e : extras.get(d).entrySet()) {
                addItem(mp, foods.get(e.getKey()), e.getValue());
            }
            main.getMealPlans().add(mp);
        }
        programmeRepository.save(main);

        programmeRepository.save(NutritionProgramme.builder().name("Anganwadi Child Nutrition Drive").type("SUPPLEMENTARY")
                .objectives("Take-home ration and hot meals for children under 6.").startDate(today.plusDays(14))
                .endDate(today.plusDays(74)).targetCategory(BeneficiaryCategory.CHILD).status(ProgrammeStatus.DRAFT).build());
        programmeRepository.save(NutritionProgramme.builder().name("Summer Breakfast Pilot").type("BREAKFAST")
                .objectives("Pilot breakfast scheme (completed).").startDate(today.minusDays(120))
                .endDate(today.minusDays(60)).targetCategory(BeneficiaryCategory.STUDENT).status(ProgrammeStatus.COMPLETED).build());
        log.info("Seeded programme data (school '{}' enrolled with {} people)", school.path("fullName").asText(), people);
    }

    private void addItem(MealPlan mp, JsonNode food, String qty) {
        if (food == null) {
            log.warn("Food item missing from food-nutrition-service - skipped in seed meal plan");
            return;
        }
        mp.getItems().add(MealPlanItem.builder().mealPlan(mp).foodItemId(food.path("id").asLong())
                .foodItemName(food.path("name").asText()).unit(FoodUnit.valueOf(food.path("unit").asText()))
                .quantityPerPerson(new java.math.BigDecimal(qty)).build());
    }
}

package com.foodsupply.programme.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.foodsupply.programme.client.BeneficiaryClient;
import com.foodsupply.programme.client.FoodClient;
import com.foodsupply.programme.client.InventoryClient;
import com.foodsupply.programme.dto.RequirementsResponse;
import com.foodsupply.programme.dto.RequirementsResponse.Nutrition;
import com.foodsupply.programme.dto.RequirementsResponse.NutritionValues;
import com.foodsupply.programme.dto.RequirementsResponse.RequiredItem;
import com.foodsupply.programme.entity.MealPlan;
import com.foodsupply.programme.entity.MealPlanItem;
import com.foodsupply.programme.entity.NutritionProgramme;
import com.foodsupply.programme.entity.Participation;
import com.foodsupply.programme.entity.ParticipationStatus;
import com.foodsupply.programme.exception.InvalidRequestException;
import com.foodsupply.programme.exception.ResourceNotFoundException;
import com.foodsupply.programme.repository.ProgrammeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequirementServiceImpl implements RequirementService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final ProgrammeRepository programmeRepository;
    private final BeneficiaryClient beneficiaryClient;
    private final FoodClient foodClient;
    private final InventoryClient inventoryClient;

    @Override
    public RequirementsResponse calculate(Long programmeId, LocalDate from, LocalDate to) {
        NutritionProgramme p = programmeRepository.findById(programmeId)
                .orElseThrow(() -> new ResourceNotFoundException("Programme not found: " + programmeId));
        LocalDate start = from != null ? from : p.getStartDate();
        LocalDate end = to != null ? to : p.getEndDate();
        if (end.isBefore(start)) {
            throw new InvalidRequestException("'to' must not be before 'from'");
        }
        if (ChronoUnit.DAYS.between(start, end) > 365) {
            throw new InvalidRequestException("Date range must not exceed 366 days");
        }

        int people = 0;
        for (Participation x : p.getParticipations()) {
            if (x.getStatus() == ParticipationStatus.ENROLLED) {
                people += x.getPeopleCovered();
            }
        }

        Map<DayOfWeek, List<MealPlanItem>> itemsByDay = new EnumMap<>(DayOfWeek.class);
        for (MealPlan mp : p.getMealPlans()) {
            itemsByDay.computeIfAbsent(mp.getDayOfWeek(), k -> new ArrayList<>()).addAll(mp.getItems());
        }

        // sum of per-person quantity over every date of the range (each date mapped to its weekday)
        Map<Long, BigDecimal> perPerson = new LinkedHashMap<>();
        Map<Long, MealPlanItem> meta = new LinkedHashMap<>();
        int days = 0;
        int serving = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            days++;
            List<MealPlanItem> list = itemsByDay.get(d.getDayOfWeek());
            if (list == null || list.isEmpty()) {
                continue;
            }
            serving++;
            for (MealPlanItem it : list) {
                perPerson.merge(it.getFoodItemId(), it.getQuantityPerPerson(), BigDecimal::add);
                meta.putIfAbsent(it.getFoodItemId(), it);
            }
        }

        // inventory availability
        Map<Long, JsonNode> avail = new HashMap<>();
        if (!perPerson.isEmpty() && people > 0) {
            List<Map<String, Object>> reqItems = new ArrayList<>();
            for (Map.Entry<Long, BigDecimal> e : perPerson.entrySet()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("foodItemId", e.getKey());
                m.put("requiredQuantity", e.getValue().multiply(BigDecimal.valueOf(people)).setScale(3, RoundingMode.HALF_UP));
                reqItems.add(m);
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("items", reqItems);
            body.put("neededUntil", end.toString());
            JsonNode res = inventoryClient.availability(body);
            for (JsonNode n : res.path("items")) {
                avail.put(n.path("foodItemId").asLong(), n);
            }
        }

        List<RequiredItem> items = new ArrayList<>();
        List<RequiredItem> shortages = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> e : perPerson.entrySet()) {
            MealPlanItem m = meta.get(e.getKey());
            BigDecimal total = e.getValue().multiply(BigDecimal.valueOf(people)).setScale(3, RoundingMode.HALF_UP);
            JsonNode a = avail.get(e.getKey());
            RequiredItem ri;
            if (a == null) {
                ri = new RequiredItem(e.getKey(), m.getFoodItemName(), m.getUnit(), e.getValue(), total,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true);
            } else {
                ri = new RequiredItem(e.getKey(), m.getFoodItemName(), m.getUnit(), e.getValue(), total,
                        a.path("usableQuantity").decimalValue(), a.path("shortage").decimalValue(),
                        a.path("expiringSoonQuantity").decimalValue(), a.path("usableByNeededDate").decimalValue(),
                        a.path("sufficient").asBoolean(true));
            }
            items.add(ri);
            if (!ri.sufficient()) {
                shortages.add(ri);
            }
        }

        Nutrition nutrition = nutrition(p, perPerson, serving);
        log.info("Requirements for programme {}: {} items, {} shortages", programmeId, items.size(), shortages.size());
        return new RequirementsResponse(p.getId(), p.getName(), start, end, days, serving, people, items, shortages,
                shortages.isEmpty(), nutrition);
    }

    private Nutrition nutrition(NutritionProgramme p, Map<Long, BigDecimal> perPerson, int serving) {
        BigDecimal zero = BigDecimal.ZERO.setScale(2);
        NutritionValues perDay = new NutritionValues(zero, zero, zero, zero, zero);
        if (!perPerson.isEmpty() && serving > 0) {
            List<Map<String, Object>> calcItems = new ArrayList<>();
            for (Map.Entry<Long, BigDecimal> e : perPerson.entrySet()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("foodItemId", e.getKey());
                m.put("quantityPerPerson", e.getValue());
                calcItems.add(m);
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("persons", 1);
            body.put("days", 1);
            body.put("items", calcItems);
            JsonNode n = foodClient.calculate(body).path("nutritionPerPersonPerDay");
            BigDecimal div = BigDecimal.valueOf(serving);
            perDay = new NutritionValues(avg(n, "calories", div), avg(n, "protein", div), avg(n, "carbs", div),
                    avg(n, "fat", div), avg(n, "fibre", div));
        }
        String category = p.getTargetCategory() == null ? null : p.getTargetCategory().name();
        NutritionValues required = null;
        NutritionValues percent = null;
        if (category != null) {
            JsonNode r = beneficiaryClient.getRequirement(category);
            if (r != null) {
                required = new NutritionValues(r.path("dailyCalories").decimalValue(), r.path("dailyProteinG").decimalValue(),
                        r.path("dailyCarbsG").decimalValue(), r.path("dailyFatG").decimalValue(), r.path("dailyFibreG").decimalValue());
                percent = new NutritionValues(pct(perDay.calories(), required.calories()), pct(perDay.protein(), required.protein()),
                        pct(perDay.carbs(), required.carbs()), pct(perDay.fat(), required.fat()), pct(perDay.fibre(), required.fibre()));
            }
        }
        return new Nutrition(category, perDay, required, percent);
    }

    private static BigDecimal avg(JsonNode n, String field, BigDecimal div) {
        return n.path(field).decimalValue().divide(div, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal pct(BigDecimal value, BigDecimal required) {
        if (required == null || required.signum() == 0) {
            return null;
        }
        return value.multiply(HUNDRED).divide(required, 1, RoundingMode.HALF_UP);
    }
}

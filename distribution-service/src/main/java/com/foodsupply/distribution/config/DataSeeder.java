package com.foodsupply.distribution.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.foodsupply.distribution.client.ProgrammeClient;
import com.foodsupply.distribution.entity.CentreStatus;
import com.foodsupply.distribution.entity.DistributionCentre;
import com.foodsupply.distribution.entity.DistributionItem;
import com.foodsupply.distribution.entity.DistributionPlan;
import com.foodsupply.distribution.entity.DistributionRecord;
import com.foodsupply.distribution.entity.FoodUnit;
import com.foodsupply.distribution.entity.PlanBeneficiary;
import com.foodsupply.distribution.entity.PlanStatus;
import com.foodsupply.distribution.entity.RecordStatus;
import com.foodsupply.distribution.repository.CentreRepository;
import com.foodsupply.distribution.repository.PlanRepository;
import com.foodsupply.distribution.repository.RecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeds centres and a few historical plans/records. Historical plans never touch inventory.
 * Plans need programme-service (programme id 1 = School Mid-Day Meal Programme) to be running for the
 * participant list; if it is down they are skipped and retried on the next start.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private final CentreRepository centreRepository;
    private final PlanRepository planRepository;
    private final RecordRepository recordRepository;
    private final ProgrammeClient programmeClient;

    @Override
    public void run(String... args) {
        if (centreRepository.count() == 0) {
            centreRepository.save(DistributionCentre.builder().name("Adyar Community Distribution Hub")
                    .address("12 Lattice Bridge Road, Adyar, Chennai").contactPerson("R. Lakshmi").phone("9840011122")
                    .status(CentreStatus.ACTIVE).build());
            centreRepository.save(DistributionCentre.builder().name("Tambaram Central Kitchen")
                    .address("45 GST Road, Tambaram, Chennai").contactPerson("S. Karthikeyan").phone("9841022233")
                    .status(CentreStatus.ACTIVE).build());
            centreRepository.save(DistributionCentre.builder().name("Anna Nagar Ration Depot")
                    .address("7 2nd Avenue, Anna Nagar, Chennai").contactPerson("M. Fathima").phone("9840033344")
                    .status(CentreStatus.ACTIVE).build());
            log.info("Seeded 3 distribution centres");
        }
        if (planRepository.count() > 0) {
            log.info("Distribution plans already present - seeding skipped");
            return;
        }
        List<DistributionCentre> centres = centreRepository.findAll();
        JsonNode participants;
        try {
            participants = programmeClient.participants(1L);
        } catch (RuntimeException e) {
            log.warn("Historical plans not seeded (start programme-service first): {}", e.getMessage());
            return;
        }
        List<JsonNode> enrolled = new ArrayList<>();
        for (JsonNode p : participants) {
            if ("ENROLLED".equals(p.path("status").asText())) {
                enrolled.add(p);
            }
        }
        if (enrolled.isEmpty() || centres.isEmpty()) {
            log.warn("Historical plans not seeded: programme 1 has no enrolled participants");
            return;
        }
        LocalDate today = LocalDate.now();
        savePlan(centres.get(0), today.minusDays(21), today.minusDays(15), PlanStatus.DELIVERED, "1.00", enrolled,
                "Week 1 - delivered in full");
        savePlan(centres.get(1 % centres.size()), today.minusDays(14), today.minusDays(8), PlanStatus.PARTIAL, "0.90", enrolled,
                "Week 2 - 10% short because of a vehicle breakdown");
        savePlan(centres.get(0), today.minusDays(7), today.minusDays(1), PlanStatus.CANCELLED, "0", enrolled,
                "Week 3 - cancelled, school closed for exam holidays");
        log.info("Seeded 3 historical distribution plans");
    }

    private void savePlan(DistributionCentre centre, LocalDate start, LocalDate end, PlanStatus status, String factor,
                          List<JsonNode> enrolled, String remarks) {
        BigDecimal f = new BigDecimal(factor);
        boolean done = status == PlanStatus.DELIVERED || status == PlanStatus.PARTIAL;
        int people = 0;
        for (JsonNode p : enrolled) {
            people += p.path("peopleCovered").asInt(1);
        }
        DistributionPlan plan = DistributionPlan.builder().programmeId(1L).programmeName("School Mid-Day Meal Programme")
                .centre(centre).startDate(start).endDate(end).status(status).remarks(remarks)
                .createdOn(start.minusDays(3)).enrolledPeople(people)
                .dispatchedOn(done ? start : null).deliveredOn(done ? end : null).build();
        Object[][] lines = {
                {1L, "Rice (Raw)", FoodUnit.KG, "350"},
                {5L, "Toor Dal", FoodUnit.KG, "140"},
                {17L, "Banana", FoodUnit.PIECE, "2000"},
                {21L, "Milk (Toned)", FoodUnit.LITRE, "400"}};
        for (Object[] l : lines) {
            BigDecimal planned = new BigDecimal((String) l[3]);
            plan.getItems().add(DistributionItem.builder().plan(plan).foodItemId((Long) l[0]).foodItemName((String) l[1])
                    .unit((FoodUnit) l[2]).plannedQty(planned).dispatchedQty(done ? planned : BigDecimal.ZERO)
                    .deliveredQty(done ? planned.multiply(f).setScale(3, RoundingMode.HALF_UP) : BigDecimal.ZERO).build());
        }
        for (JsonNode p : enrolled) {
            plan.getBeneficiaries().add(PlanBeneficiary.builder().plan(plan).beneficiaryId(p.path("beneficiaryId").asLong())
                    .beneficiaryName(p.path("beneficiaryName").asText()).peopleCovered(p.path("peopleCovered").asInt(1)).build());
        }
        DistributionPlan saved = planRepository.save(plan);
        saved.setPlanNumber(String.format("DP-%d-%04d", saved.getCreatedOn().getYear(), saved.getId()));
        planRepository.save(saved);
        if (!done) {
            return;
        }
        List<DistributionRecord> records = new ArrayList<>();
        for (JsonNode p : enrolled) {
            for (DistributionItem it : saved.getItems()) {
                BigDecimal share = it.getDeliveredQty().multiply(BigDecimal.valueOf(p.path("peopleCovered").asInt(1)))
                        .divide(BigDecimal.valueOf(Math.max(people, 1)), 3, RoundingMode.HALF_UP);
                records.add(DistributionRecord.builder().plan(saved).beneficiaryId(p.path("beneficiaryId").asLong())
                        .beneficiaryName(p.path("beneficiaryName").asText()).foodItemId(it.getFoodItemId())
                        .foodItemName(it.getFoodItemName()).unit(it.getUnit()).quantity(share).deliveredOn(end)
                        .status(status == PlanStatus.DELIVERED ? RecordStatus.DELIVERED : RecordStatus.PARTIAL).build());
            }
        }
        recordRepository.saveAll(records);
    }
}

package com.foodsupply.quality.config;

import com.foodsupply.quality.entity.ActionStatus;
import com.foodsupply.quality.entity.CorrectiveAction;
import com.foodsupply.quality.entity.FoodUnit;
import com.foodsupply.quality.entity.InspectionResult;
import com.foodsupply.quality.entity.QualityInspection;
import com.foodsupply.quality.entity.QualityStatus;
import com.foodsupply.quality.entity.WastageReason;
import com.foodsupply.quality.entity.WastageRecord;
import com.foodsupply.quality.repository.CorrectiveActionRepository;
import com.foodsupply.quality.repository.QualityInspectionRepository;
import com.foodsupply.quality.repository.WastageRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Random;

/**
 * Seeds historical quality data. It never calls inventory-service, so batch ids and numbers
 * below are illustrative history only (food item ids/names match the food-nutrition seed).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private final QualityInspectionRepository inspectionRepository;
    private final WastageRecordRepository wastageRepository;
    private final CorrectiveActionRepository actionRepository;

    @Override
    public void run(String... args) {
        if (inspectionRepository.count() > 0 || wastageRepository.count() > 0 || actionRepository.count() > 0) {
            log.info("Quality data already present, seeding skipped");
            return;
        }

        Random random = new Random(42);
        LocalDate today = LocalDate.now();

        // ---- Inspections -------------------------------------------------------------
        QualityInspection soya = inspection(today.minusDays(2), 9L, "Soya Chunks", 9L, "SOYA-2026-001",
                "Meena Krishnan", InspectionResult.FAIL, QualityStatus.QUARANTINED,
                "Off smell and moisture above limit; held for lab test");
        QualityInspection curd = inspection(today.minusDays(3), 22L, "Curd (Yogurt)", 22L, "CURD-2026-001",
                "R. Suresh Kumar", InspectionResult.FAIL, QualityStatus.REJECTED,
                "Past expiry and sour; cold chain break suspected");
        QualityInspection chicken = inspection(today.minusDays(5), 26L, "Chicken (Broiler)", 26L, "CHKN-2026-002",
                "Dr. Anitha Rajan", InspectionResult.CONDITIONAL, QualityStatus.APPROVED,
                "Approved for immediate use only; re-inspect in 2 days");
        inspection(today.minusDays(7), 1L, "Rice (Raw)", 1L, "RICE-2026-004",
                "Meena Krishnan", InspectionResult.PASS, QualityStatus.APPROVED, "Clean, dry, no infestation");
        inspection(today.minusDays(10), 21L, "Milk (Toned)", 21L, "MILK-2026-006",
                "R. Suresh Kumar", InspectionResult.PASS, QualityStatus.APPROVED, "Temperature 4 C, acidity normal");
        inspection(today.minusDays(14), 12L, "Tomato", 12L, "TOMA-2026-003",
                "Dr. Anitha Rajan", InspectionResult.CONDITIONAL, QualityStatus.APPROVED,
                "Some soft fruits removed; remainder fit for use");
        QualityInspection spinach = inspection(today.minusDays(21), 14L, "Spinach (Palak)", 14L, "SPIN-2026-002",
                "Meena Krishnan", InspectionResult.FAIL, QualityStatus.REJECTED, "Wilted and slimy leaves");
        inspection(today.minusDays(28), 25L, "Egg", 25L, "EGG-2026-005",
                "R. Suresh Kumar", InspectionResult.PASS, QualityStatus.APPROVED, "Candling test passed");
        inspection(today.minusDays(35), 17L, "Banana", 17L, "BANA-2026-004",
                "Dr. Anitha Rajan", InspectionResult.PASS, QualityStatus.APPROVED, "Firm, correct ripeness");

        // ---- Wastage records ---------------------------------------------------------
        waste(today.minusDays(4), 22L, "Curd (Yogurt)", 22L, "CURD-2026-001", FoodUnit.KG,
                BigDecimal.valueOf(40), WastageReason.EXPIRED, "Municipal disposal", "Store Manager",
                "Expired before issue");
        WastageRecord milk = waste(today.minusDays(9), 21L, "Milk (Toned)", 21L, "MILK-2026-003", FoodUnit.LITRE,
                BigDecimal.valueOf(55 + random.nextInt(10)), WastageReason.SPOILED, "Drain disposal",
                "Store Manager", "Cold room 2 compressor failure overnight");
        WastageRecord tomato = waste(today.minusDays(18), 12L, "Tomato", 12L, "TOMA-2026-003", FoodUnit.KG,
                BigDecimal.valueOf(30 + random.nextInt(10)), WastageReason.SPOILED, "Composting",
                "Store Manager", "Over-ripe stock discarded");
        waste(today.minusDays(33), 17L, "Banana", 17L, "BANA-2026-002", FoodUnit.PIECE,
                BigDecimal.valueOf(220 + random.nextInt(40)), WastageReason.DAMAGED, "Composting",
                "Store Assistant", "Crushed in transit");
        waste(today.minusDays(47), 14L, "Spinach (Palak)", 14L, "SPIN-2026-002", FoodUnit.KG,
                BigDecimal.valueOf(15 + random.nextInt(8)), WastageReason.SPOILED, "Composting",
                "Store Manager", "Rejected at inspection");
        waste(today.minusDays(62), 25L, "Egg", 25L, "EGG-2026-002", FoodUnit.PIECE,
                BigDecimal.valueOf(110 + random.nextInt(30)), WastageReason.DAMAGED, "Municipal disposal",
                "Store Assistant", "Broken trays during unloading");
        waste(today.minusDays(80), 26L, "Chicken (Broiler)", 26L, "CHKN-2026-001", FoodUnit.KG,
                BigDecimal.valueOf(10 + random.nextInt(5)), WastageReason.CONTAMINATED, "Incineration",
                "Store Manager", "Failed microbial test");
        waste(today.minusDays(95), 1L, "Rice (Raw)", 1L, "RICE-2026-001", FoodUnit.KG,
                BigDecimal.valueOf(25), WastageReason.OTHER, "Returned to supplier", "Store Manager",
                "Pest infestation in few bags");

        // ---- Corrective actions ------------------------------------------------------
        action(soya.getId(), null, "Re-test sample at an accredited lab and decide on return to supplier",
                "Meena Krishnan", today.plusDays(5), ActionStatus.OPEN, null);
        action(curd.getId(), null, "Review cold-chain temperature logs and supplier delivery times",
                "R. Suresh Kumar", today.minusDays(3), ActionStatus.IN_PROGRESS, null);
        action(chicken.getId(), null, "Re-inspect chicken stock and issue on FEFO basis",
                "Dr. Anitha Rajan", today.plusDays(2), ActionStatus.OPEN, null);
        action(spinach.getId(), null, "Switch leafy vegetable deliveries to daily small lots",
                "Meena Krishnan", today.minusDays(14), ActionStatus.CLOSED, today.minusDays(16));
        action(null, milk.getId(), "Install temperature alarm and service the compressor in cold room 2",
                "Maintenance Team", today.plusDays(10), ActionStatus.OPEN, null);
        action(null, tomato.getId(), "Introduce weekly FEFO check for perishable vegetables",
                "Store Manager", today.minusDays(8), ActionStatus.CLOSED, today.minusDays(10));

        log.info("Seeded quality data: {} inspections, {} wastage records, {} corrective actions",
                inspectionRepository.count(), wastageRepository.count(), actionRepository.count());
    }

    private QualityInspection inspection(LocalDate date, Long foodItemId, String foodName, Long batchId,
                                         String batchNumber, String inspector, InspectionResult result,
                                         QualityStatus status, String remarks) {
        return inspectionRepository.save(QualityInspection.builder()
                .inspectionDate(date)
                .foodItemId(foodItemId)
                .foodItemName(foodName)
                .batchId(batchId)
                .batchNumber(batchNumber)
                .inspector(inspector)
                .result(result)
                .qualityStatus(status)
                .remarks(remarks)
                .build());
    }

    private WastageRecord waste(LocalDate date, Long foodItemId, String foodName, Long batchId, String batchNumber,
                                FoodUnit unit, BigDecimal quantity, WastageReason reason, String disposal,
                                String recordedBy, String remarks) {
        return wastageRepository.save(WastageRecord.builder()
                .recordedOn(date)
                .foodItemId(foodItemId)
                .foodItemName(foodName)
                .batchId(batchId)
                .batchNumber(batchNumber)
                .unit(unit)
                .quantity(quantity)
                .reason(reason)
                .disposalMethod(disposal)
                .recordedBy(recordedBy)
                .remarks(remarks)
                .build());
    }

    private void action(Long inspectionId, Long wastageId, String description, String assignedTo, LocalDate dueDate,
                        ActionStatus status, LocalDate completedOn) {
        actionRepository.save(CorrectiveAction.builder()
                .inspectionId(inspectionId)
                .wastageId(wastageId)
                .description(description)
                .assignedTo(assignedTo)
                .dueDate(dueDate)
                .status(status)
                .createdOn(LocalDate.now().minusDays(20))
                .completedOn(completedOn)
                .build());
    }
}

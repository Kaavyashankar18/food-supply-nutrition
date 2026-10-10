package com.foodsupply.beneficiary.config;

import com.foodsupply.beneficiary.entity.Beneficiary;
import com.foodsupply.beneficiary.entity.BeneficiaryCategory;
import com.foodsupply.beneficiary.entity.BeneficiaryStatus;
import com.foodsupply.beneficiary.entity.BeneficiaryType;
import com.foodsupply.beneficiary.entity.NutritionRequirement;
import com.foodsupply.beneficiary.repository.BeneficiaryRepository;
import com.foodsupply.beneficiary.repository.NutritionRequirementRepository;
import com.foodsupply.beneficiary.service.EligibilityRules;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Seeds nutrition requirements and a realistic population of beneficiaries (default 500)
 * so the "500 beneficiaries for next week" scenario works out of the box.
 * Runs only when the tables are empty. Disable with app.seed.enabled=false.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private static final String[] FIRST_NAMES = {"Aarav", "Priya", "Karthik", "Lakshmi", "Murugan", "Divya", "Ravi",
            "Meena", "Suresh", "Anitha", "Vignesh", "Kavitha", "Arun", "Deepa", "Senthil", "Revathi", "Gopal",
            "Saranya", "Bala", "Nandhini", "Manoj", "Pooja", "Harish", "Janani", "Dinesh", "Shalini", "Ramesh",
            "Sangeetha", "Naveen", "Geetha"};
    private static final String[] LAST_NAMES = {"Kumar", "Raj", "Devi", "Subramanian", "Iyer", "Pillai", "Nair",
            "Reddy", "Krishnan", "Selvam", "Balaji", "Natarajan", "Ganesan", "Sundaram", "Rajan"};
    private static final String[] AREAS = {"Anna Nagar", "T. Nagar", "Adyar", "Velachery", "Tambaram", "Perambur",
            "Guindy", "Mylapore", "Porur", "Ambattur", "Sholinganallur", "Chromepet"};
    private static final String[] CONDITIONS = {"Anaemia", "Diabetes", "Tuberculosis recovery", "Malnutrition",
            "Hypertension", "Post-surgery recovery"};
    private static final String[] DIETS = {"VEGETARIAN", "NON_VEGETARIAN", "VEGAN"};

    private final BeneficiaryRepository beneficiaryRepository;
    private final NutritionRequirementRepository requirementRepository;
    private final EligibilityRules eligibilityRules;

    @Value("${app.seed.enabled:true}")
    private boolean enabled;

    @Value("${app.seed.beneficiary-count:500}")
    private int total;

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }
        if (requirementRepository.count() == 0) {
            seedRequirements();
        }
        if (beneficiaryRepository.count() == 0) {
            seedBeneficiaries();
        }
        if (!beneficiaryRepository.existsByType(BeneficiaryType.SCHOOL)) {
            seedInstitutions();
        }
    }

    /** The demo scenario: a school with 500 students, plus a few other group beneficiaries. */
    private void seedInstitutions() {
        List<Beneficiary> list = List.of(
                institution("Government Higher Secondary School, Adyar", BeneficiaryType.SCHOOL,
                        BeneficiaryCategory.STUDENT, 500, "12, Gandhi Road, Adyar, Chennai", "Egg allergy: 12 students"),
                institution("Sunrise Orphanage", BeneficiaryType.ORPHANAGE,
                        BeneficiaryCategory.CHILD, 80, "45, Church Street, Perambur, Chennai", null),
                institution("Anbu Community Centre", BeneficiaryType.COMMUNITY_CENTRE,
                        BeneficiaryCategory.ELDERLY, 120, "7, Temple Road, Mylapore, Chennai", "Low salt, soft food"),
                institution("Selvam Family", BeneficiaryType.FAMILY,
                        BeneficiaryCategory.CHILD, 5, "22, Market Street, Tambaram, Chennai", null));
        beneficiaryRepository.saveAll(list);
        log.info("Seeded {} institutional beneficiaries", list.size());
    }

    private Beneficiary institution(String name, BeneficiaryType type, BeneficiaryCategory category,
                                    int people, String address, String restrictions) {
        Beneficiary b = Beneficiary.builder()
                .fullName(name).type(type).peopleServed(people).category(category)
                .address(address).contactNumber("9444000" + (100 + people % 900))
                .dietaryPreference("VEGETARIAN").dietaryRestrictions(restrictions)
                .status(BeneficiaryStatus.ACTIVE).registeredOn(LocalDate.now().minusDays(30))
                .build();
        EligibilityRules.Result r = eligibilityRules.evaluate(b);
        b.setEligible(r.eligible());
        b.setEligibilityNotes(r.reason());
        b.setEligibilityVerifiedOn(LocalDate.now().minusDays(30));
        return b;
    }

    private void seedRequirements() {
        List<NutritionRequirement> list = List.of(
                req(BeneficiaryCategory.CHILD, 1400, "35", "190", "40", "20", "Ages 0-14"),
                req(BeneficiaryCategory.STUDENT, 2200, "55", "300", "60", "28", "Ages 5-25"),
                req(BeneficiaryCategory.PREGNANT_WOMAN, 2500, "70", "330", "70", "30", "Extra iron and folate"),
                req(BeneficiaryCategory.LACTATING_MOTHER, 2700, "75", "360", "75", "30", "Extra protein and fluids"),
                req(BeneficiaryCategory.ELDERLY, 1900, "55", "260", "50", "30", "Soft, high-fibre foods"),
                req(BeneficiaryCategory.PATIENT, 2100, "65", "280", "55", "25", "Adjust to the medical condition"));
        requirementRepository.saveAll(list);
        log.info("Seeded {} nutrition requirements", list.size());
    }

    private NutritionRequirement req(BeneficiaryCategory c, int kcal, String protein, String carbs,
                                     String fat, String fibre, String notes) {
        return NutritionRequirement.builder().category(c).dailyCalories(kcal)
                .dailyProteinG(new BigDecimal(protein)).dailyCarbsG(new BigDecimal(carbs))
                .dailyFatG(new BigDecimal(fat)).dailyFibreG(new BigDecimal(fibre)).notes(notes).build();
    }

    private void seedBeneficiaries() {
        // Share of the population per category (adds up to 1.0)
        Map<BeneficiaryCategory, Double> mix = new java.util.LinkedHashMap<>();
        mix.put(BeneficiaryCategory.CHILD, 0.24);
        mix.put(BeneficiaryCategory.STUDENT, 0.24);
        mix.put(BeneficiaryCategory.PREGNANT_WOMAN, 0.12);
        mix.put(BeneficiaryCategory.LACTATING_MOTHER, 0.10);
        mix.put(BeneficiaryCategory.ELDERLY, 0.20);
        mix.put(BeneficiaryCategory.PATIENT, 0.10);

        Random rnd = new Random(42); // fixed seed: same demo data on every machine
        List<Beneficiary> batch = new ArrayList<>(total);
        for (Map.Entry<BeneficiaryCategory, Double> e : mix.entrySet()) {
            int n = (int) Math.round(total * e.getValue());
            for (int i = 0; i < n && batch.size() < total; i++) {
                batch.add(build(e.getKey(), rnd));
            }
        }
        while (batch.size() < total) { // rounding top-up
            batch.add(build(BeneficiaryCategory.STUDENT, rnd));
        }
        beneficiaryRepository.saveAll(batch);
        log.info("Seeded {} beneficiaries", batch.size());
    }

    private Beneficiary build(BeneficiaryCategory category, Random rnd) {
        boolean female = category == BeneficiaryCategory.PREGNANT_WOMAN
                || category == BeneficiaryCategory.LACTATING_MOTHER || rnd.nextBoolean();
        int age = switch (category) {
            case CHILD -> 1 + rnd.nextInt(14);
            case STUDENT -> 6 + rnd.nextInt(19);
            case PREGNANT_WOMAN, LACTATING_MOTHER -> 19 + rnd.nextInt(18);
            case ELDERLY -> 60 + rnd.nextInt(30);
            case PATIENT -> 18 + rnd.nextInt(60);
        };
        Beneficiary b = Beneficiary.builder()
                .fullName(FIRST_NAMES[rnd.nextInt(FIRST_NAMES.length)] + " " + LAST_NAMES[rnd.nextInt(LAST_NAMES.length)])
                .type(BeneficiaryType.INDIVIDUAL)
                .peopleServed(1)
                .age(age)
                .gender(female ? "FEMALE" : "MALE")
                .category(category)
                .address((1 + rnd.nextInt(120)) + ", " + AREAS[rnd.nextInt(AREAS.length)] + ", Chennai")
                .contactNumber("9" + (100000000 + rnd.nextInt(899999999)))
                .householdSize(2 + rnd.nextInt(6))
                .healthCondition(category == BeneficiaryCategory.PATIENT
                        ? CONDITIONS[rnd.nextInt(CONDITIONS.length)]
                        : (rnd.nextInt(10) == 0 ? "Anaemia" : null))
                .dietaryPreference(DIETS[rnd.nextInt(10) < 7 ? 0 : (rnd.nextInt(10) < 9 ? 1 : 2)])
                // about 4% inactive so filters and eligibility checks have something to show
                .status(rnd.nextInt(100) < 4 ? BeneficiaryStatus.INACTIVE : BeneficiaryStatus.ACTIVE)
                .registeredOn(LocalDate.now().minusDays(rnd.nextInt(365)))
                .build();
        EligibilityRules.Result r = eligibilityRules.evaluate(b);
        b.setEligible(r.eligible());
        b.setEligibilityNotes(r.reason());
        b.setEligibilityVerifiedOn(b.getRegisteredOn());
        return b;
    }
}

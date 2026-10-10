package com.foodsupply.programme.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodsupply.programme.client.BeneficiaryClient;
import com.foodsupply.programme.client.FoodClient;
import com.foodsupply.programme.client.InventoryClient;
import com.foodsupply.programme.dto.RequirementsResponse;
import com.foodsupply.programme.entity.FoodUnit;
import com.foodsupply.programme.entity.MealPlan;
import com.foodsupply.programme.entity.MealPlanItem;
import com.foodsupply.programme.entity.MealType;
import com.foodsupply.programme.entity.NutritionProgramme;
import com.foodsupply.programme.entity.Participation;
import com.foodsupply.programme.entity.ParticipationStatus;
import com.foodsupply.programme.entity.ProgrammeStatus;
import com.foodsupply.programme.exception.InvalidRequestException;
import com.foodsupply.programme.repository.ProgrammeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequirementServiceImplTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private ProgrammeRepository programmeRepository;
    @Mock
    private BeneficiaryClient beneficiaryClient;
    @Mock
    private FoodClient foodClient;
    @Mock
    private InventoryClient inventoryClient;
    @InjectMocks
    private RequirementServiceImpl service;

    private NutritionProgramme programmeWithMondayRice() {
        NutritionProgramme p = NutritionProgramme.builder().id(1L).name("School Mid-Day Meal")
                .startDate(LocalDate.of(2026, 10, 12)).endDate(LocalDate.of(2026, 10, 18))
                .status(ProgrammeStatus.ACTIVE).build();
        p.getParticipations().add(Participation.builder().programme(p).beneficiaryId(1L).beneficiaryName("School A")
                .peopleCovered(500).status(ParticipationStatus.ENROLLED).build());
        MealPlan mp = MealPlan.builder().programme(p).dayOfWeek(DayOfWeek.MONDAY).mealType(MealType.MID_DAY_MEAL).build();
        mp.getItems().add(MealPlanItem.builder().mealPlan(mp).foodItemId(1L).foodItemName("Rice (Raw)")
                .unit(FoodUnit.KG).quantityPerPerson(new BigDecimal("0.100")).build());
        p.getMealPlans().add(mp);
        return p;
    }

    @Test
    void calculate_multipliesQuantityByPeopleAndReportsShortage() throws Exception {
        when(programmeRepository.findById(1L)).thenReturn(Optional.of(programmeWithMondayRice()));
        when(inventoryClient.availability(any())).thenReturn(MAPPER.readTree(
                "{\"allSufficient\":false,\"items\":[{\"foodItemId\":1,\"usableQuantity\":30,\"shortage\":20,"
                        + "\"expiringSoonQuantity\":0,\"usableByNeededDate\":30,\"sufficient\":false}]}"));
        when(foodClient.calculate(any())).thenReturn(MAPPER.readTree(
                "{\"nutritionPerPersonPerDay\":{\"calories\":350,\"protein\":8,\"carbs\":77,\"fat\":1,\"fibre\":2}}"));

        RequirementsResponse r = service.calculate(1L, null, null);

        assertEquals(7, r.days());
        assertEquals(1, r.servingDays());
        assertEquals(500, r.enrolledPeople());
        assertEquals(1, r.items().size());
        assertEquals(0, new BigDecimal("50.000").compareTo(r.items().get(0).totalQuantity()));
        assertFalse(r.allSufficient());
        assertEquals(1, r.shortages().size());
        assertEquals(0, new BigDecimal("350").compareTo(r.nutrition().perPersonPerDay().calories()));
    }

    @Test
    void calculate_rejectsInvertedDateRange() {
        when(programmeRepository.findById(1L)).thenReturn(Optional.of(programmeWithMondayRice()));

        assertThrows(InvalidRequestException.class,
                () -> service.calculate(1L, LocalDate.of(2026, 10, 18), LocalDate.of(2026, 10, 12)));
    }
}

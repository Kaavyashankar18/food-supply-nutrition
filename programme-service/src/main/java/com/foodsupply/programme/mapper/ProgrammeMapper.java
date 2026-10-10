package com.foodsupply.programme.mapper;

import com.foodsupply.programme.dto.MealItemResponse;
import com.foodsupply.programme.dto.MealPlanResponse;
import com.foodsupply.programme.dto.ParticipationResponse;
import com.foodsupply.programme.dto.ProgrammeResponse;
import com.foodsupply.programme.entity.MealPlan;
import com.foodsupply.programme.entity.NutritionProgramme;
import com.foodsupply.programme.entity.Participation;
import com.foodsupply.programme.entity.ParticipationStatus;

import java.util.List;

public final class ProgrammeMapper {

    private ProgrammeMapper() {
    }

    public static ProgrammeResponse toResponse(NutritionProgramme p) {
        int participants = 0;
        int people = 0;
        for (Participation x : p.getParticipations()) {
            if (x.getStatus() == ParticipationStatus.ENROLLED) {
                participants++;
                people += x.getPeopleCovered();
            }
        }
        return new ProgrammeResponse(p.getId(), p.getName(), p.getType(), p.getObjectives(), p.getStartDate(),
                p.getEndDate(), p.getTargetCategory(), p.getStatus(), participants, people, p.getMealPlans().size());
    }

    public static ParticipationResponse toResponse(Participation x) {
        return new ParticipationResponse(x.getId(), x.getProgramme().getId(), x.getBeneficiaryId(),
                x.getBeneficiaryName(), x.getPeopleCovered(), x.getEnrolledOn(), x.getStatus());
    }

    public static MealPlanResponse toResponse(MealPlan m) {
        List<MealItemResponse> items = m.getItems().stream()
                .map(i -> new MealItemResponse(i.getFoodItemId(), i.getFoodItemName(), i.getUnit(), i.getQuantityPerPerson()))
                .toList();
        return new MealPlanResponse(m.getId(), m.getProgramme().getId(), m.getDayOfWeek(), m.getMealType(), items);
    }
}

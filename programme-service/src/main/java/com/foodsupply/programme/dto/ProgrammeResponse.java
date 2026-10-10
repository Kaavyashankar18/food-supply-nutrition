package com.foodsupply.programme.dto;

import com.foodsupply.programme.entity.BeneficiaryCategory;
import com.foodsupply.programme.entity.ProgrammeStatus;

import java.time.LocalDate;

public record ProgrammeResponse(Long id, String name, String type, String objectives, LocalDate startDate,
                                LocalDate endDate, BeneficiaryCategory targetCategory, ProgrammeStatus status,
                                int participantCount, int enrolledPeople, int mealPlanCount) {
}

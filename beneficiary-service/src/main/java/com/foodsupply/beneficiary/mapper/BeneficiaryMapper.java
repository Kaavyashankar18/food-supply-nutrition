package com.foodsupply.beneficiary.mapper;

import com.foodsupply.beneficiary.dto.BeneficiaryDto;
import com.foodsupply.beneficiary.dto.NutritionRequirementDto;
import com.foodsupply.beneficiary.entity.Beneficiary;
import com.foodsupply.beneficiary.entity.BeneficiaryType;
import com.foodsupply.beneficiary.entity.NutritionRequirement;

/** Entities never leave the service layer; everything crossing the API goes through here. */
public final class BeneficiaryMapper {

    private BeneficiaryMapper() {
    }

    public static BeneficiaryDto toDto(Beneficiary b) {
        return BeneficiaryDto.builder()
                .id(b.getId())
                .fullName(b.getFullName())
                .type(b.getType())
                .peopleServed(b.getPeopleServed())
                .age(b.getAge())
                .gender(b.getGender())
                .category(b.getCategory())
                .address(b.getAddress())
                .contactNumber(b.getContactNumber())
                .householdSize(b.getHouseholdSize())
                .healthCondition(b.getHealthCondition())
                .dietaryPreference(b.getDietaryPreference())
                .dietaryRestrictions(b.getDietaryRestrictions())
                .status(b.getStatus())
                .eligible(b.isEligible())
                .eligibilityNotes(b.getEligibilityNotes())
                .eligibilityVerifiedOn(b.getEligibilityVerifiedOn())
                .registeredOn(b.getRegisteredOn())
                .build();
    }

    /**
     * Copies editable fields only; id, eligibility and registeredOn are controlled by the service.
     * The service validates the dto first, so type and peopleServed are consistent here.
     */
    public static void copyEditable(BeneficiaryDto dto, Beneficiary b) {
        BeneficiaryType type = dto.getType() == null ? BeneficiaryType.INDIVIDUAL : dto.getType();
        b.setFullName(dto.getFullName().trim());
        b.setType(type);
        b.setPeopleServed(type == BeneficiaryType.INDIVIDUAL ? Integer.valueOf(1) : dto.getPeopleServed());
        b.setAge(type == BeneficiaryType.INDIVIDUAL ? dto.getAge() : null);
        b.setGender(type == BeneficiaryType.INDIVIDUAL ? dto.getGender() : null);
        b.setCategory(dto.getCategory());
        b.setAddress(dto.getAddress());
        b.setContactNumber(dto.getContactNumber());
        b.setHouseholdSize(dto.getHouseholdSize());
        b.setHealthCondition(dto.getHealthCondition());
        b.setDietaryPreference(dto.getDietaryPreference());
        b.setDietaryRestrictions(dto.getDietaryRestrictions());
        if (dto.getStatus() != null) {
            b.setStatus(dto.getStatus());
        }
    }

    public static NutritionRequirementDto toDto(NutritionRequirement r) {
        return NutritionRequirementDto.builder()
                .id(r.getId())
                .category(r.getCategory())
                .dailyCalories(r.getDailyCalories())
                .dailyProteinG(r.getDailyProteinG())
                .dailyCarbsG(r.getDailyCarbsG())
                .dailyFatG(r.getDailyFatG())
                .dailyFibreG(r.getDailyFibreG())
                .notes(r.getNotes())
                .build();
    }

    public static void copyEditable(NutritionRequirementDto dto, NutritionRequirement r) {
        r.setCategory(dto.getCategory());
        r.setDailyCalories(dto.getDailyCalories());
        r.setDailyProteinG(dto.getDailyProteinG());
        r.setDailyCarbsG(dto.getDailyCarbsG());
        r.setDailyFatG(dto.getDailyFatG());
        r.setDailyFibreG(dto.getDailyFibreG());
        r.setNotes(dto.getNotes());
    }
}

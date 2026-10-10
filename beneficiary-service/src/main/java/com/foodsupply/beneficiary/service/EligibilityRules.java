package com.foodsupply.beneficiary.service;

import com.foodsupply.beneficiary.entity.Beneficiary;
import com.foodsupply.beneficiary.entity.BeneficiaryStatus;
import com.foodsupply.beneficiary.entity.BeneficiaryType;
import org.springframework.stereotype.Component;

/**
 * Single place for programme eligibility rules. To change a rule or add a category,
 * edit only this class.
 */
@Component
public class EligibilityRules {

    public record Result(boolean eligible, String reason) {
    }

    public Result evaluate(Beneficiary b) {
        if (b.getStatus() == BeneficiaryStatus.INACTIVE) {
            return no("Beneficiary is inactive");
        }

        BeneficiaryType type = b.getType() == null ? BeneficiaryType.INDIVIDUAL : b.getType();
        if (type != BeneficiaryType.INDIVIDUAL) {
            // Groups are eligible when they serve at least one person; age and gender do not apply.
            int people = b.getPeopleServed() == null ? 0 : b.getPeopleServed();
            String label = type.name().replace('_', ' ').toLowerCase();
            return people >= 1
                    ? ok("Registered " + label + " serving " + people + " people")
                    : no("A " + label + " must serve at least 1 person");
        }

        int age = b.getAge() == null ? -1 : b.getAge();
        boolean female = "FEMALE".equalsIgnoreCase(b.getGender());

        return switch (b.getCategory()) {
            case CHILD -> age >= 0 && age <= 14
                    ? ok("Child aged 0-14")
                    : no("Child category requires age 0-14");
            case STUDENT -> age >= 5 && age <= 25
                    ? ok("Student aged 5-25")
                    : no("Student category requires age 5-25");
            case PREGNANT_WOMAN -> female && age >= 15 && age <= 49
                    ? ok("Pregnant woman aged 15-49")
                    : no("Pregnant woman category requires a female aged 15-49");
            case LACTATING_MOTHER -> female && age >= 15 && age <= 49
                    ? ok("Lactating mother aged 15-49")
                    : no("Lactating mother category requires a female aged 15-49");
            case ELDERLY -> age >= 60
                    ? ok("Elderly aged 60 or above")
                    : no("Elderly category requires age 60 or above");
            case PATIENT -> b.getHealthCondition() != null && !b.getHealthCondition().isBlank()
                    ? ok("Patient with recorded health condition")
                    : no("Patient category requires a recorded health condition");
        };
    }

    private Result ok(String reason) {
        return new Result(true, reason);
    }

    private Result no(String reason) {
        return new Result(false, reason);
    }
}

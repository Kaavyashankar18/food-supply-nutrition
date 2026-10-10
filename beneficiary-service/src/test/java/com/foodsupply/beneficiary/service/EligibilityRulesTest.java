package com.foodsupply.beneficiary.service;

import com.foodsupply.beneficiary.entity.Beneficiary;
import com.foodsupply.beneficiary.entity.BeneficiaryCategory;
import com.foodsupply.beneficiary.entity.BeneficiaryStatus;
import com.foodsupply.beneficiary.entity.BeneficiaryType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EligibilityRulesTest {

    private final EligibilityRules rules = new EligibilityRules();

    private Beneficiary person(BeneficiaryCategory c, int age, String gender, String condition, BeneficiaryStatus s) {
        return Beneficiary.builder().type(BeneficiaryType.INDIVIDUAL).peopleServed(1)
                .category(c).age(age).gender(gender).healthCondition(condition).status(s).build();
    }

    private Beneficiary group(BeneficiaryType type, int people, BeneficiaryStatus s) {
        return Beneficiary.builder().type(type).peopleServed(people)
                .category(BeneficiaryCategory.STUDENT).status(s).build();
    }

    @Test
    void childUnder15IsEligible() {
        assertTrue(rules.evaluate(person(BeneficiaryCategory.CHILD, 8, "MALE", null, BeneficiaryStatus.ACTIVE)).eligible());
    }

    @Test
    void childOver14IsNotEligible() {
        assertFalse(rules.evaluate(person(BeneficiaryCategory.CHILD, 16, "MALE", null, BeneficiaryStatus.ACTIVE)).eligible());
    }

    @Test
    void elderlyNeeds60Plus() {
        assertTrue(rules.evaluate(person(BeneficiaryCategory.ELDERLY, 70, "FEMALE", null, BeneficiaryStatus.ACTIVE)).eligible());
        assertFalse(rules.evaluate(person(BeneficiaryCategory.ELDERLY, 55, "FEMALE", null, BeneficiaryStatus.ACTIVE)).eligible());
    }

    @Test
    void pregnantWomanMustBeFemale() {
        assertTrue(rules.evaluate(person(BeneficiaryCategory.PREGNANT_WOMAN, 28, "FEMALE", null, BeneficiaryStatus.ACTIVE)).eligible());
        assertFalse(rules.evaluate(person(BeneficiaryCategory.PREGNANT_WOMAN, 28, "MALE", null, BeneficiaryStatus.ACTIVE)).eligible());
    }

    @Test
    void patientNeedsHealthCondition() {
        assertTrue(rules.evaluate(person(BeneficiaryCategory.PATIENT, 40, "MALE", "Anaemia", BeneficiaryStatus.ACTIVE)).eligible());
        assertFalse(rules.evaluate(person(BeneficiaryCategory.PATIENT, 40, "MALE", " ", BeneficiaryStatus.ACTIVE)).eligible());
    }

    @Test
    void inactiveIsNeverEligible() {
        assertFalse(rules.evaluate(person(BeneficiaryCategory.CHILD, 8, "MALE", null, BeneficiaryStatus.INACTIVE)).eligible());
        assertFalse(rules.evaluate(group(BeneficiaryType.SCHOOL, 500, BeneficiaryStatus.INACTIVE)).eligible());
    }

    @Test
    void schoolWithStudentsIsEligibleWithoutAge() {
        assertTrue(rules.evaluate(group(BeneficiaryType.SCHOOL, 500, BeneficiaryStatus.ACTIVE)).eligible());
    }

    @Test
    void groupServingNobodyIsNotEligible() {
        assertFalse(rules.evaluate(group(BeneficiaryType.ORPHANAGE, 0, BeneficiaryStatus.ACTIVE)).eligible());
    }

    @Test
    void missingTypeIsTreatedAsIndividual() {
        Beneficiary b = Beneficiary.builder().category(BeneficiaryCategory.CHILD).age(5).gender("MALE")
                .status(BeneficiaryStatus.ACTIVE).build();
        assertTrue(rules.evaluate(b).eligible());
    }
}

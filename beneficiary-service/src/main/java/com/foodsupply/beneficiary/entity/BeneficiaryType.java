package com.foodsupply.beneficiary.entity;

/** INDIVIDUAL records one person; every other type is a group served as one unit (see peopleServed). */
public enum BeneficiaryType {
    INDIVIDUAL,
    FAMILY,
    SCHOOL,
    ORPHANAGE,
    COMMUNITY_CENTRE
}

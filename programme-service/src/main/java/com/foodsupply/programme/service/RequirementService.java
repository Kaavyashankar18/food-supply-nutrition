package com.foodsupply.programme.service;

import com.foodsupply.programme.dto.RequirementsResponse;

import java.time.LocalDate;

public interface RequirementService {

    RequirementsResponse calculate(Long programmeId, LocalDate from, LocalDate to);
}

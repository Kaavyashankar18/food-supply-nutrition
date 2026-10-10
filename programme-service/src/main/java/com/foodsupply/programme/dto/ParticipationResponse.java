package com.foodsupply.programme.dto;

import com.foodsupply.programme.entity.ParticipationStatus;

import java.time.LocalDate;

public record ParticipationResponse(Long id, Long programmeId, Long beneficiaryId, String beneficiaryName,
                                    int peopleCovered, LocalDate enrolledOn, ParticipationStatus status) {
}

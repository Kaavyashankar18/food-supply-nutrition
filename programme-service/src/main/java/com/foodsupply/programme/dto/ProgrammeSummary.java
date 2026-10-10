package com.foodsupply.programme.dto;

import java.util.Map;

public record ProgrammeSummary(long totalProgrammes, long activeProgrammes, Map<String, Long> byStatus,
                               long activeParticipants, long peopleCoveredInActiveProgrammes) {
}

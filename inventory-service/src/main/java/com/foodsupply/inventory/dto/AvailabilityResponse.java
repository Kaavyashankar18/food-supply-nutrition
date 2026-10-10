package com.foodsupply.inventory.dto;

import java.time.LocalDate;
import java.util.List;

public record AvailabilityResponse(LocalDate neededUntil, boolean allSufficient, List<ItemAvailability> items) {
}

package com.foodsupply.distribution.dto;

import com.foodsupply.distribution.entity.FoodUnit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Result of /generate: nothing is saved. */
public record PreviewResponse(Long programmeId, String programmeName, Long centreId, String centreName,
                              LocalDate startDate, LocalDate endDate, int enrolledPeople,
                              List<PreviewItem> items, List<PreviewItem> shortages, List<PreviewItem> expiringItems,
                              boolean allSufficient) {

    public record PreviewItem(Long foodItemId, String foodItemName, FoodUnit unit, BigDecimal plannedQty,
                              BigDecimal usableQty, BigDecimal shortage, BigDecimal expiringSoonQty,
                              boolean sufficient) {
    }
}

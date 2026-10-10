package com.foodsupply.procurement.dto;

import com.foodsupply.procurement.entity.RequestSource;
import com.foodsupply.procurement.entity.RequestStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RequestResponse(Long id, String requestNumber, RequestStatus status, RequestSource source,
                              String reason, BigDecimal estimatedCost, String requestedBy, LocalDate createdOn,
                              LocalDate needByDate, String decisionRemarks, List<RequestItemResponse> items) {
}

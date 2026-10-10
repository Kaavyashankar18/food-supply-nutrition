package com.foodsupply.inventory.service;

import java.math.BigDecimal;
import java.util.List;

import com.foodsupply.inventory.dto.AvailabilityRequest;
import com.foodsupply.inventory.dto.AvailabilityResponse;
import com.foodsupply.inventory.dto.BatchResponse;
import com.foodsupply.inventory.dto.PageResponse;
import com.foodsupply.inventory.dto.ReceiveStockRequest;
import com.foodsupply.inventory.dto.ReferenceRequest;
import com.foodsupply.inventory.dto.ReservationResponse;
import com.foodsupply.inventory.dto.ReservationResultResponse;
import com.foodsupply.inventory.dto.ReserveRequest;
import com.foodsupply.inventory.dto.StockLevelResponse;
import com.foodsupply.inventory.dto.StockSummaryResponse;

public interface StockService {

    PageResponse<StockLevelResponse> levels(String q, Boolean lowStock, int page, int size, String sort,
            String dir);

    StockLevelResponse level(Long foodItemId);

    List<StockLevelResponse> lowStock();

    StockLevelResponse setMinimumLevel(Long foodItemId, BigDecimal value);

    BatchResponse receive(ReceiveStockRequest request);

    AvailabilityResponse availability(AvailabilityRequest request);

    ReservationResultResponse reserve(ReserveRequest request);

    ReservationResultResponse release(ReferenceRequest request);

    ReservationResultResponse dispatch(ReferenceRequest request);

    List<ReservationResponse> reservations(String referenceType, Long referenceId);

    StockSummaryResponse summary();
}

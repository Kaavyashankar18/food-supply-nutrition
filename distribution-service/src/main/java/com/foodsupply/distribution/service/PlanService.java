package com.foodsupply.distribution.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.foodsupply.distribution.dto.DeliveryRequest;
import com.foodsupply.distribution.dto.DistributionSummary;
import com.foodsupply.distribution.dto.PageResponse;
import com.foodsupply.distribution.dto.PlanListItem;
import com.foodsupply.distribution.dto.PlanRequest;
import com.foodsupply.distribution.dto.PlanResponse;
import com.foodsupply.distribution.dto.PreviewResponse;
import com.foodsupply.distribution.dto.RaiseProcurementRequest;
import com.foodsupply.distribution.entity.PlanStatus;

public interface PlanService {

    PageResponse<PlanListItem> list(String q, PlanStatus status, Long programmeId, Long centreId,
                                    int page, int size, String sort, String dir);

    PlanResponse get(Long id);

    PreviewResponse preview(PlanRequest request);

    PlanResponse create(PlanRequest request);

    PlanResponse dispatch(Long id);

    PlanResponse deliver(Long id, DeliveryRequest request);

    PlanResponse cancel(Long id);

    JsonNode raiseProcurement(RaiseProcurementRequest request);

    DistributionSummary summary();
}

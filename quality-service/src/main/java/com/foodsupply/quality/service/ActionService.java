package com.foodsupply.quality.service;

import com.foodsupply.quality.dto.ActionRequest;
import com.foodsupply.quality.dto.ActionResponse;
import com.foodsupply.quality.dto.PageResponse;
import com.foodsupply.quality.entity.ActionStatus;

public interface ActionService {

    PageResponse<ActionResponse> list(String q, Long inspectionId, Long wastageId, ActionStatus status,
                                      Boolean overdue, int page, int size, String sort, String dir);

    ActionResponse get(Long id);

    ActionResponse create(ActionRequest request);

    ActionResponse update(Long id, ActionRequest request);

    ActionResponse changeStatus(Long id, ActionStatus status);

    void delete(Long id);
}

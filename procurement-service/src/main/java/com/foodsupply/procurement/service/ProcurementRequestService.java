package com.foodsupply.procurement.service;

import com.foodsupply.procurement.dto.PageResponse;
import com.foodsupply.procurement.dto.ProcurementRequestInput;
import com.foodsupply.procurement.dto.RequestResponse;
import com.foodsupply.procurement.entity.RequestSource;
import com.foodsupply.procurement.entity.RequestStatus;

public interface ProcurementRequestService {

    PageResponse<RequestResponse> list(String q, RequestStatus status, RequestSource source, int page, int size,
                                       String sort, String dir);

    RequestResponse get(Long id);

    RequestResponse create(ProcurementRequestInput input);

    RequestResponse update(Long id, ProcurementRequestInput input);

    void delete(Long id);

    RequestResponse submit(Long id);

    RequestResponse approve(Long id);

    RequestResponse reject(Long id, String reason);
}

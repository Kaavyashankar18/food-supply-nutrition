package com.foodsupply.procurement.service;

import com.foodsupply.procurement.dto.PageResponse;
import com.foodsupply.procurement.dto.PurchaseOrderInput;
import com.foodsupply.procurement.dto.PurchaseOrderResponse;
import com.foodsupply.procurement.dto.ReceiveInput;
import com.foodsupply.procurement.entity.PurchaseOrderStatus;

public interface PurchaseOrderService {

    PageResponse<PurchaseOrderResponse> list(String q, PurchaseOrderStatus status, Long supplierId, Long requestId,
                                             int page, int size, String sort, String dir);

    PurchaseOrderResponse get(Long id);

    PurchaseOrderResponse create(PurchaseOrderInput input);

    PurchaseOrderResponse receive(Long id, ReceiveInput input);

    PurchaseOrderResponse updateStatus(Long id, PurchaseOrderStatus status);
}

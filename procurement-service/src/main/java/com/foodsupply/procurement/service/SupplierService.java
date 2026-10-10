package com.foodsupply.procurement.service;

import com.foodsupply.procurement.dto.PageResponse;
import com.foodsupply.procurement.dto.SupplierRequest;
import com.foodsupply.procurement.dto.SupplierResponse;
import com.foodsupply.procurement.entity.SupplierStatus;

public interface SupplierService {

    PageResponse<SupplierResponse> list(String q, SupplierStatus status, int page, int size, String sort, String dir);

    SupplierResponse get(Long id);

    SupplierResponse create(SupplierRequest request);

    SupplierResponse update(Long id, SupplierRequest request);

    void delete(Long id);

    SupplierResponse updateStatus(Long id, SupplierStatus status);
}

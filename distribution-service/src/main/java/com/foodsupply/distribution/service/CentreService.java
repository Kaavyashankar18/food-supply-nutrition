package com.foodsupply.distribution.service;

import com.foodsupply.distribution.dto.CentreRequest;
import com.foodsupply.distribution.dto.CentreResponse;
import com.foodsupply.distribution.dto.PageResponse;
import com.foodsupply.distribution.entity.CentreStatus;

public interface CentreService {

    PageResponse<CentreResponse> list(String q, CentreStatus status, int page, int size, String sort, String dir);

    CentreResponse get(Long id);

    CentreResponse create(CentreRequest request);

    CentreResponse update(Long id, CentreRequest request);

    void delete(Long id);
}

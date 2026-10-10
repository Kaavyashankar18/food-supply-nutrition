package com.foodsupply.distribution.dto;

import com.foodsupply.distribution.entity.CentreStatus;

public record CentreResponse(Long id, String name, String address, String contactPerson, String phone,
                             CentreStatus status) {
}

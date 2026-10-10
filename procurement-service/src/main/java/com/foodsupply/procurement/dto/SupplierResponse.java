package com.foodsupply.procurement.dto;

import com.foodsupply.procurement.entity.SupplierStatus;

import java.math.BigDecimal;

public record SupplierResponse(Long id, String name, String contactPerson, String phone, String email,
                               String address, SupplierStatus status, BigDecimal rating) {
}

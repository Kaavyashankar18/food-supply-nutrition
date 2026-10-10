package com.foodsupply.procurement.mapper;

import com.foodsupply.procurement.dto.SupplierRequest;
import com.foodsupply.procurement.dto.SupplierResponse;
import com.foodsupply.procurement.entity.Supplier;
import com.foodsupply.procurement.entity.SupplierStatus;

public final class SupplierMapper {

    private SupplierMapper() {
    }

    public static SupplierResponse toResponse(Supplier s) {
        return new SupplierResponse(s.getId(), s.getName(), s.getContactPerson(), s.getPhone(), s.getEmail(),
                s.getAddress(), s.getStatus(), s.getRating());
    }

    public static Supplier toEntity(SupplierRequest r) {
        Supplier s = new Supplier();
        apply(s, r);
        return s;
    }

    public static void apply(Supplier s, SupplierRequest r) {
        s.setName(r.getName().trim());
        s.setContactPerson(r.getContactPerson());
        s.setPhone(r.getPhone());
        s.setEmail(r.getEmail());
        s.setAddress(r.getAddress());
        s.setStatus(r.getStatus() != null ? r.getStatus() : SupplierStatus.ACTIVE);
        s.setRating(r.getRating());
    }
}

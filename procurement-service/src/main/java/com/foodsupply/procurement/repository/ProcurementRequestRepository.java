package com.foodsupply.procurement.repository;

import com.foodsupply.procurement.entity.ProcurementRequest;
import com.foodsupply.procurement.entity.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ProcurementRequestRepository
        extends JpaRepository<ProcurementRequest, Long>, JpaSpecificationExecutor<ProcurementRequest> {

    Optional<ProcurementRequest> findTopByRequestNumberStartingWithOrderByRequestNumberDesc(String prefix);

    long countByStatus(RequestStatus status);
}

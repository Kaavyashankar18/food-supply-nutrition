package com.foodsupply.procurement.repository;

import com.foodsupply.procurement.entity.PurchaseOrder;
import com.foodsupply.procurement.entity.PurchaseOrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PurchaseOrderRepository
        extends JpaRepository<PurchaseOrder, Long>, JpaSpecificationExecutor<PurchaseOrder> {

    Optional<PurchaseOrder> findTopByPoNumberStartingWithOrderByPoNumberDesc(String prefix);

    long countByStatus(PurchaseOrderStatus status);

    boolean existsBySupplierId(Long supplierId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PurchaseOrder p where p.id = :id")
    Optional<PurchaseOrder> findByIdForUpdate(@Param("id") Long id);
}

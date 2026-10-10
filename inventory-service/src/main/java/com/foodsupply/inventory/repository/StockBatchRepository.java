package com.foodsupply.inventory.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.foodsupply.inventory.entity.QualityStatus;
import com.foodsupply.inventory.entity.StockBatch;

import jakarta.persistence.LockModeType;

public interface StockBatchRepository extends JpaRepository<StockBatch, Long>, JpaSpecificationExecutor<StockBatch> {

    boolean existsByBatchNumberIgnoreCase(String batchNumber);

    List<StockBatch> findByFoodItemIdIn(Collection<Long> foodItemIds);

    /** Locks the rows: approved, non-expired batches that still have unreserved stock. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from StockBatch b where b.foodItemId = :foodItemId and b.qualityStatus = :status "
            + "and b.quantityAvailable > b.quantityReserved "
            + "and (b.expiryDate is null or b.expiryDate >= :today)")
    List<StockBatch> findAllocatable(@Param("foodItemId") Long foodItemId, @Param("status") QualityStatus status,
            @Param("today") LocalDate today);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from StockBatch b where b.id in :ids")
    List<StockBatch> findAllByIdForUpdate(@Param("ids") Collection<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from StockBatch b where b.id = :id")
    Optional<StockBatch> findByIdForUpdate(@Param("id") Long id);

    List<StockBatch> findByExpiryDateBetweenAndQuantityAvailableGreaterThanOrderByExpiryDateAsc(LocalDate from,
            LocalDate to, BigDecimal minQuantity);

    List<StockBatch> findByExpiryDateBeforeAndQuantityAvailableGreaterThanOrderByExpiryDateAsc(LocalDate before,
            BigDecimal minQuantity);
}

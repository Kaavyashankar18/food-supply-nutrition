package com.foodsupply.inventory.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.foodsupply.inventory.entity.Reservation;
import com.foodsupply.inventory.entity.ReservationStatus;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    boolean existsByReferenceTypeAndReferenceIdAndStatus(String referenceType, Long referenceId,
            ReservationStatus status);

    List<Reservation> findByReferenceTypeAndReferenceIdAndStatus(String referenceType, Long referenceId,
            ReservationStatus status);

    List<Reservation> findByReferenceTypeAndReferenceIdOrderByIdAsc(String referenceType, Long referenceId);

    long countByStatus(ReservationStatus status);
}

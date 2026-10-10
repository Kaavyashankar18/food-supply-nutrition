package com.foodsupply.quality.repository;

import com.foodsupply.quality.entity.ActionStatus;
import com.foodsupply.quality.entity.CorrectiveAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;

public interface CorrectiveActionRepository
        extends JpaRepository<CorrectiveAction, Long>, JpaSpecificationExecutor<CorrectiveAction> {

    long countByStatus(ActionStatus status);

    long countByStatusNot(ActionStatus status);

    long countByStatusNotAndDueDateBefore(ActionStatus status, LocalDate date);
}

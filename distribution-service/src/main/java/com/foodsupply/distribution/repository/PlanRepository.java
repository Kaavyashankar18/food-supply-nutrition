package com.foodsupply.distribution.repository;

import com.foodsupply.distribution.entity.DistributionPlan;
import com.foodsupply.distribution.entity.PlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PlanRepository extends JpaRepository<DistributionPlan, Long>, JpaSpecificationExecutor<DistributionPlan> {
    long countByStatus(PlanStatus status);

    long countByCentreId(Long centreId);
}

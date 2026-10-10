package com.foodsupply.distribution.repository;

import com.foodsupply.distribution.entity.CentreStatus;
import com.foodsupply.distribution.entity.DistributionCentre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CentreRepository extends JpaRepository<DistributionCentre, Long>, JpaSpecificationExecutor<DistributionCentre> {
    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    long countByStatus(CentreStatus status);
}

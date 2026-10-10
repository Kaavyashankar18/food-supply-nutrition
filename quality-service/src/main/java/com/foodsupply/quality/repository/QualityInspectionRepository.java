package com.foodsupply.quality.repository;

import com.foodsupply.quality.entity.InspectionResult;
import com.foodsupply.quality.entity.QualityInspection;
import com.foodsupply.quality.entity.QualityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface QualityInspectionRepository
        extends JpaRepository<QualityInspection, Long>, JpaSpecificationExecutor<QualityInspection> {

    long countByResult(InspectionResult result);

    long countByQualityStatus(QualityStatus qualityStatus);
}

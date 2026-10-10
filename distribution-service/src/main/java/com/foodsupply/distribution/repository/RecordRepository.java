package com.foodsupply.distribution.repository;

import com.foodsupply.distribution.entity.DistributionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RecordRepository extends JpaRepository<DistributionRecord, Long>, JpaSpecificationExecutor<DistributionRecord> {
}

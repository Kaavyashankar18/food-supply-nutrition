package com.foodsupply.quality.repository;

import com.foodsupply.quality.entity.WastageRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface WastageRecordRepository
        extends JpaRepository<WastageRecord, Long>, JpaSpecificationExecutor<WastageRecord> {
}

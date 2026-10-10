package com.foodsupply.inventory.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.foodsupply.inventory.dto.BatchResponse;
import com.foodsupply.inventory.entity.StockBatch;
import com.foodsupply.inventory.util.StockMath;

public final class BatchMapper {

    private BatchMapper() {
    }

    public static BatchResponse toResponse(StockBatch b, String foodItemName, LocalDate today) {
        StockMath.Bucket bucket = StockMath.bucket(b, today);
        BigDecimal usable = bucket == StockMath.Bucket.USABLE ? StockMath.unreserved(b) : BigDecimal.ZERO;
        Integer days = b.getExpiryDate() == null ? null
                : (int) ChronoUnit.DAYS.between(today, b.getExpiryDate());
        return new BatchResponse(b.getId(), b.getFoodItemId(), foodItemName, b.getBatchNumber(), b.getSupplierId(),
                b.getQuantityReceived(), b.getQuantityAvailable(), b.getQuantityReserved(), usable,
                b.getReceivedDate(), b.getExpiryDate(), days, bucket == StockMath.Bucket.EXPIRED,
                b.getStorageLocation(), b.getQualityStatus());
    }
}

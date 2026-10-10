package com.foodsupply.inventory.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;

import com.foodsupply.inventory.entity.StockBatch;

public final class StockMath {

    public enum Bucket {
        USABLE, EXPIRED, QUARANTINED, REJECTED
    }

    /** First-expiry-first-out; batches without expiry date go last. */
    public static final Comparator<StockBatch> FEFO = Comparator
            .comparing(StockBatch::getExpiryDate, Comparator.nullsLast(Comparator.<LocalDate>naturalOrder()))
            .thenComparing(StockBatch::getReceivedDate, Comparator.nullsLast(Comparator.<LocalDate>naturalOrder()))
            .thenComparing(StockBatch::getId, Comparator.nullsLast(Comparator.<Long>naturalOrder()));

    private StockMath() {
    }

    public static Bucket bucket(StockBatch b, LocalDate today) {
        if (b.getExpiryDate() != null && b.getExpiryDate().isBefore(today)) {
            return Bucket.EXPIRED;
        }
        return switch (b.getQualityStatus()) {
            case APPROVED -> Bucket.USABLE;
            case QUARANTINED -> Bucket.QUARANTINED;
            case REJECTED -> Bucket.REJECTED;
        };
    }

    /** on hand minus reserved, never negative. */
    public static BigDecimal unreserved(StockBatch b) {
        BigDecimal v = b.getQuantityAvailable().subtract(b.getQuantityReserved());
        return v.signum() < 0 ? BigDecimal.ZERO : v;
    }
}

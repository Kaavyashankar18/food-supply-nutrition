package com.foodsupply.beneficiary.exception;

/** A rule of the domain was violated (duplicate, invalid state...). Mapped to HTTP 409. */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}

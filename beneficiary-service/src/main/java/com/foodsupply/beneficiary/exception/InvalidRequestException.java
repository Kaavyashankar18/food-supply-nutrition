package com.foodsupply.beneficiary.exception;

/** The request is well-formed but breaks a cross-field rule. Mapped to HTTP 400. */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}

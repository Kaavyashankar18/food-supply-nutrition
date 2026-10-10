package com.foodsupply.programme.exception;

public class DownstreamServiceException extends RuntimeException {
    public DownstreamServiceException(String message) {
        super(message);
    }
}

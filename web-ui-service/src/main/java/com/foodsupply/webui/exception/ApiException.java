package com.foodsupply.webui.exception;

/** A 4xx answer from a backend service (validation, not found, conflict). */
public class ApiException extends RuntimeException {

    private final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}

package com.abms.vehicle.exception;

public class DuplicateLicensePlateException extends RuntimeException {

    public DuplicateLicensePlateException(String message) {
        super(message);
    }
}
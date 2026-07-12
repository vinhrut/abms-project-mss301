package com.abms.vehicle.exception;

public class InvalidVehicleStatusException extends RuntimeException {

    public InvalidVehicleStatusException(String message) {
        super(message);
    }
}
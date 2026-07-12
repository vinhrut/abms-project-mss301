package com.abms.vehicle.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateLicensePlateException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateLicensePlate(DuplicateLicensePlateException ex) {
        return buildBadRequest(ex.getMessage());
    }

    @ExceptionHandler(LimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleLimitExceeded(LimitExceededException ex) {
        return buildBadRequest(ex.getMessage());
    }

    @ExceptionHandler(InvalidVehicleStatusException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidVehicleStatus(InvalidVehicleStatusException ex) {
        return buildBadRequest(ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        return buildBadRequest(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("message", "Validation failed");
        body.put("errors", errors);
        return ResponseEntity.badRequest().body(body);
    }

    private ResponseEntity<Map<String, Object>> buildBadRequest(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("message", message);
        return ResponseEntity.badRequest().body(body);
    }
}
package com.csms.report.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.time.DateTimeException;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(
            IllegalArgumentException exception
    ) {
        return ResponseEntity.badRequest().body(
                createError(
                        HttpStatus.BAD_REQUEST,
                        exception.getMessage()
                )
        );
    }

    @ExceptionHandler(DateTimeException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidDate(
            DateTimeException exception
    ) {
        return ResponseEntity.badRequest().body(
                createError(HttpStatus.BAD_REQUEST, exception.getMessage())
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException exception
    ) {
        Map<String, String> validationErrors =
                new LinkedHashMap<>();

        for (FieldError fieldError :
                exception.getBindingResult().getFieldErrors()) {
            validationErrors.put(
                    fieldError.getField(),
                    fieldError.getDefaultMessage()
            );
        }

        Map<String, Object> body = createError(
                HttpStatus.BAD_REQUEST,
                "Invalid report parameters"
        );

        body.put("validationErrors", validationErrors);

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
            ConstraintViolationException exception
    ) {
        return ResponseEntity.badRequest().body(
                createError(HttpStatus.BAD_REQUEST, "Invalid report parameters")
        );
    }

    @ExceptionHandler({
            MissingRequestHeaderException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<Map<String, Object>> handleMissingOrMalformedRequest(
            Exception exception
    ) {
        return ResponseEntity.badRequest().body(
                createError(HttpStatus.BAD_REQUEST, exception.getMessage())
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(
            ResponseStatusException exception
    ) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return ResponseEntity.status(status).body(
                createError(status, exception.getReason())
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            NoResourceFoundException exception
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                createError(HttpStatus.NOT_FOUND, "Resource not found")
        );
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDatabaseFailure(
            DataAccessException exception
    ) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                createError(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Report database is unavailable or its schema is not initialized"
                )
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(
            Exception exception
    ) {
        return ResponseEntity.status(
                HttpStatus.INTERNAL_SERVER_ERROR
        ).body(
                createError(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Unexpected server error"
                )
        );
    }

    private Map<String, Object> createError(
            HttpStatus status,
            String message
    ) {
        Map<String, Object> body = new LinkedHashMap<>();

        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);

        return body;
    }
}

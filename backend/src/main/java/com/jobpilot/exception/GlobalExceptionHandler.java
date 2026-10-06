package com.jobpilot.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException exception) {

        Map<String, String> errors = new HashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.put(error.getField(), error.getDefaultMessage())
                );

        Map<String, Object> response = new HashMap<>();

        response.put("status", 400);
        response.put("message", "Validation failed");
        response.put("errors", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException exception) {

        Map<String, Object> response = new HashMap<>();
        
        // If it's a "not found" state, we can return 404
        boolean isNotFound = exception.getMessage().contains("not found") ||
                             exception.getMessage().contains("Not found");
        
        int status = isNotFound ? 404 : 400;
        HttpStatus httpStatus = isNotFound ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;

        response.put("status", status);
        response.put("message", exception.getMessage());

        return ResponseEntity
                .status(httpStatus)
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception exception) {
        
        // Print the traceback to the terminal so we can read it instantly!
        exception.printStackTrace();

        Map<String, Object> response = new HashMap<>();
        response.put("status", 500);
        response.put("message", "Internal server error: " + exception.getMessage());
        response.put("error", exception.getClass().getSimpleName());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}
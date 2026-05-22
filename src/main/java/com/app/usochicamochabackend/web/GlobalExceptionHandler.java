package com.app.usochicamochabackend.web;

import com.app.usochicamochabackend.exception.ResourceNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

import java.util.Locale;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<String> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<String> handleAuthenticationException(
            org.springframework.security.core.AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
    }

    /**
     * Debe ir antes que {@link #handleGeneralException}: sin esto, un 400/409 por nombre duplicado
     * acababa como 500 "Unexpected error: 400 BAD_REQUEST \"...\"".
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handleResponseStatus(ResponseStatusException ex) {
        HttpStatusCode code = ex.getStatusCode();
        HttpStatus status = HttpStatus.resolve(code.value());
        HttpStatus resolved = status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
        String reason = ex.getReason();
        String body = (reason != null && !reason.isBlank()) ? reason : resolved.getReasonPhrase();
        return ResponseEntity.status(resolved).body(body);
    }

    /** Respaldo si la restricción UNIQUE salta en BD sin comprobación previa en servicio. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDataIntegrity(DataIntegrityViolationException ex) {
        Throwable cause = ex.getMostSpecificCause();
        String raw = cause != null && cause.getMessage() != null ? cause.getMessage() : ex.getMessage();
        String lower = raw != null ? raw.toLowerCase(Locale.ROOT) : "";
        if (lower.contains("unique")
                || lower.contains("duplicate")
                || lower.contains("violates unique constraint")
                || lower.contains("uk_")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Ya existe un registro con ese nombre.");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("No se pudo guardar el registro por restricciones de datos.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneralException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error: " + ex.getMessage());
    }
}

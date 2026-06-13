package com.app.usochicamochabackend.web;

import com.app.usochicamochabackend.exception.ResourceNotFoundException;
import com.app.usochicamochabackend.exception.UserSoftDeletedConflictException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.HttpMediaTypeNotSupportedException;

import java.util.stream.Collectors;

import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Maneja IllegalArgumentException: validaciones fallidas
     * Devuelve 400 BAD_REQUEST con el mensaje de error específico
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", ex.getMessage() != null ? ex.getMessage() : "Argumento inválido");
        response.put("status", HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<String> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(UserSoftDeletedConflictException.class)
    public ResponseEntity<Map<String, Object>> handleUserSoftDeletedConflict(UserSoftDeletedConflictException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", ex.getMessage());
        response.put("softDeletedUser", ex.getSoftDeletedUser());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
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

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Tipo de contenido no soportado. Use application/json");
        response.put("contentType", ex.getContentType());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneralException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error: " + ex.getMessage());
    }
}

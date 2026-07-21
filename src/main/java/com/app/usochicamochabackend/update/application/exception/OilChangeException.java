package com.app.usochicamochabackend.update.application.exception;

/**
 * Excepción base para errores en cambios de aceite
 */
public abstract class OilChangeException extends RuntimeException {
    public OilChangeException(String message) {
        super(message);
    }

    public OilChangeException(String message, Throwable cause) {
        super(message, cause);
    }
}

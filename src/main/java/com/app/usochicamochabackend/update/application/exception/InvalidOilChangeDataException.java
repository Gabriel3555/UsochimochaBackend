package com.app.usochicamochabackend.update.application.exception;

/**
 * Se lanza cuando los datos del cambio de aceite son inválidos
 * (ej: km negativo, intervalo inválido, cantidad negativa)
 */
public class InvalidOilChangeDataException extends OilChangeException {
    public InvalidOilChangeDataException(String message) {
        super(message);
    }

    public InvalidOilChangeDataException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.app.usochicamochabackend.update.application.exception;

/**
 * Se lanza cuando no se encuentra el vehículo especificado
 */
public class VehicleNotFoundException extends OilChangeException {
    public VehicleNotFoundException(String placa) {
        super("Vehículo con placa '" + placa + "' no encontrado");
    }

    public VehicleNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

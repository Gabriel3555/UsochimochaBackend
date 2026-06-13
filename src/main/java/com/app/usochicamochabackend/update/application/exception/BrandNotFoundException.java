package com.app.usochicamochabackend.update.application.exception;

/**
 * Se lanza cuando no se encuentra la marca de aceite especificada
 */
public class BrandNotFoundException extends OilChangeException {
    public BrandNotFoundException(Long brandId) {
        super("Marca de aceite con ID " + brandId + " no encontrada");
    }

    public BrandNotFoundException(String message) {
        super(message);
    }
}

package com.app.usochicamochabackend.update.application.service;

import com.app.usochicamochabackend.common.text.InputTextNormalizer;
import com.app.usochicamochabackend.update.application.dto.VehicleOilChangeRequest;
import com.app.usochicamochabackend.update.application.exception.InvalidOilChangeDataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Servicio de validación para cambios de aceite
 * Valida datos de entrada y lanza excepciones de dominio específicas
 */
@Service
public class OilChangeValidationService {

    private static final Logger logger = LoggerFactory.getLogger(OilChangeValidationService.class);

    private static final int MIN_KM = 0;
    private static final int MAX_KM = 999999;
    private static final int MIN_INTERVAL_KM = 100;
    private static final int MAX_INTERVAL_KM = 100000;
    private static final double MIN_QUANTITY = 0.5;
    private static final double MAX_QUANTITY = 100.0;

    /**
     * Valida un request de cambio de aceite
     * @param request Los datos a validar
     * @throws InvalidOilChangeDataException si algún dato es inválido
     */
    public void validateOilChangeRequest(VehicleOilChangeRequest request) {
        logger.debug("Validando cambio de aceite para vehículo: {}", request.placa());

        validatePlaca(request.placa());
        validateKmAtChange(request.kmAtChange());
        validateIntervalKm(request.intervalKm());
        validateQuantity(request.quantity());
        validateOilType(request.oilType());

        logger.debug("Validación completada exitosamente para vehículo: {}", request.placa());
    }

    private void validatePlaca(String placa) {
        if (placa == null || placa.trim().isEmpty()) {
            logger.warn("Intento de validar cambio de aceite sin placa");
            throw new InvalidOilChangeDataException("Placa es obligatoria");
        }

        String normalized = InputTextNormalizer.normalizePlaca(placa);
        if (normalized.isEmpty()) {
            logger.warn("Placa inválida proporcionada: {}", placa);
            throw new InvalidOilChangeDataException("Placa con formato inválido: " + placa);
        }
    }

    private void validateKmAtChange(Integer kmAtChange) {
        if (kmAtChange == null) {
            logger.warn("Intento de validar cambio de aceite sin km");
            throw new InvalidOilChangeDataException("Kilómetro del cambio es obligatorio");
        }

        if (kmAtChange < MIN_KM) {
            logger.warn("Km del cambio negativo: {}", kmAtChange);
            throw new InvalidOilChangeDataException(
                "Kilómetro del cambio no puede ser negativo: " + kmAtChange
            );
        }

        if (kmAtChange > MAX_KM) {
            logger.warn("Km del cambio excede máximo permitido: {}", kmAtChange);
            throw new InvalidOilChangeDataException(
                "Kilómetro del cambio excede el máximo permitido: " + MAX_KM
            );
        }
    }

    private void validateIntervalKm(Integer intervalKm) {
        if (intervalKm == null) {
            logger.warn("Intento de validar cambio de aceite sin intervalo");
            throw new InvalidOilChangeDataException("Intervalo de cambio es obligatorio");
        }

        if (intervalKm < MIN_INTERVAL_KM) {
            logger.warn("Intervalo demasiado bajo: {}", intervalKm);
            throw new InvalidOilChangeDataException(
                "Intervalo de cambio debe ser al menos " + MIN_INTERVAL_KM + " km, recibido: " + intervalKm
            );
        }

        if (intervalKm > MAX_INTERVAL_KM) {
            logger.warn("Intervalo excede máximo: {}", intervalKm);
            throw new InvalidOilChangeDataException(
                "Intervalo de cambio no puede exceder " + MAX_INTERVAL_KM + " km, recibido: " + intervalKm
            );
        }
    }

    private void validateQuantity(Double quantity) {
        if (quantity == null) {
            logger.warn("Intento de validar cambio de aceite sin cantidad");
            throw new InvalidOilChangeDataException("Cantidad de aceite es obligatoria");
        }

        if (quantity < MIN_QUANTITY) {
            logger.warn("Cantidad de aceite demasiado baja: {}", quantity);
            throw new InvalidOilChangeDataException(
                "Cantidad de aceite debe ser al menos " + MIN_QUANTITY + " litros, recibido: " + quantity
            );
        }

        if (quantity > MAX_QUANTITY) {
            logger.warn("Cantidad de aceite excede máximo: {}", quantity);
            throw new InvalidOilChangeDataException(
                "Cantidad de aceite no puede exceder " + MAX_QUANTITY + " litros, recibido: " + quantity
            );
        }
    }

    private void validateOilType(String oilType) {
        if (oilType == null || oilType.trim().isEmpty()) {
            logger.warn("Intento de validar cambio de aceite sin tipo");
            throw new InvalidOilChangeDataException("Tipo de aceite es obligatorio");
        }

        String upper = oilType.trim().toUpperCase();
        if (!upper.contains("MOTOR") && !upper.contains("HYDRAULIC")) {
            logger.warn("Tipo de aceite inválido: {}", oilType);
            throw new InvalidOilChangeDataException(
                "Tipo de aceite debe ser MOTOR o HYDRAULIC, recibido: " + oilType
            );
        }
    }
}

package com.app.usochicamochabackend.update.application.service;

import com.app.usochicamochabackend.update.application.dto.VehicleOilChangeRequest;
import com.app.usochicamochabackend.update.application.exception.InvalidOilChangeDataException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de validación para cambios de aceite
 */
public class OilChangeValidationServiceTest {

    private OilChangeValidationService validationService;

    private VehicleOilChangeRequest validRequest;

    @BeforeEach
    public void setUp() {
        validationService = new OilChangeValidationService();
        validRequest = new VehicleOilChangeRequest(
            "ABC123",
            LocalDateTime.now(),
            "MOTOR",
            1L,
            5.0,
            50000,
            3000,
            false
        );
    }

    @Test
    public void testValidRequestPasses() {
        assertDoesNotThrow(() -> validationService.validateOilChangeRequest(validRequest));
    }

    @Test
    public void testInvalidPlaca_Null() {
        VehicleOilChangeRequest request = new VehicleOilChangeRequest(
            null, validRequest.dateStamp(), validRequest.oilType(),
            validRequest.brandId(), validRequest.quantity(),
            validRequest.kmAtChange(), validRequest.intervalKm(),
            validRequest.airFilterChanged()
        );

        assertThrows(
            InvalidOilChangeDataException.class,
            () -> validationService.validateOilChangeRequest(request),
            "Debería fallar con placa null"
        );
    }

    @Test
    public void testInvalidKmAtChange_Negative() {
        VehicleOilChangeRequest request = new VehicleOilChangeRequest(
            validRequest.placa(), validRequest.dateStamp(), validRequest.oilType(),
            validRequest.brandId(), validRequest.quantity(),
            -100, validRequest.intervalKm(),
            validRequest.airFilterChanged()
        );

        assertThrows(
            InvalidOilChangeDataException.class,
            () -> validationService.validateOilChangeRequest(request),
            "Debería fallar con km negativo"
        );
    }

    @Test
    public void testInvalidKmAtChange_TooHigh() {
        VehicleOilChangeRequest request = new VehicleOilChangeRequest(
            validRequest.placa(), validRequest.dateStamp(), validRequest.oilType(),
            validRequest.brandId(), validRequest.quantity(),
            2000000, validRequest.intervalKm(),
            validRequest.airFilterChanged()
        );

        assertThrows(
            InvalidOilChangeDataException.class,
            () -> validationService.validateOilChangeRequest(request),
            "Debería fallar con km demasiado alto"
        );
    }

    @Test
    public void testInvalidIntervalKm_TooLow() {
        VehicleOilChangeRequest request = new VehicleOilChangeRequest(
            validRequest.placa(), validRequest.dateStamp(), validRequest.oilType(),
            validRequest.brandId(), validRequest.quantity(),
            validRequest.kmAtChange(), 50,
            validRequest.airFilterChanged()
        );

        assertThrows(
            InvalidOilChangeDataException.class,
            () -> validationService.validateOilChangeRequest(request),
            "Debería fallar con intervalo demasiado bajo"
        );
    }

    @Test
    public void testInvalidQuantity_TooLow() {
        VehicleOilChangeRequest request = new VehicleOilChangeRequest(
            validRequest.placa(), validRequest.dateStamp(), validRequest.oilType(),
            validRequest.brandId(), 0.1,
            validRequest.kmAtChange(), validRequest.intervalKm(),
            validRequest.airFilterChanged()
        );

        assertThrows(
            InvalidOilChangeDataException.class,
            () -> validationService.validateOilChangeRequest(request),
            "Debería fallar con cantidad demasiado baja"
        );
    }

    @Test
    public void testInvalidQuantity_TooHigh() {
        VehicleOilChangeRequest request = new VehicleOilChangeRequest(
            validRequest.placa(), validRequest.dateStamp(), validRequest.oilType(),
            validRequest.brandId(), 150.0,
            validRequest.kmAtChange(), validRequest.intervalKm(),
            validRequest.airFilterChanged()
        );

        assertThrows(
            InvalidOilChangeDataException.class,
            () -> validationService.validateOilChangeRequest(request),
            "Debería fallar con cantidad demasiado alta"
        );
    }

    @Test
    public void testInvalidOilType_InvalidValue() {
        VehicleOilChangeRequest request = new VehicleOilChangeRequest(
            validRequest.placa(), validRequest.dateStamp(), "VEGETABLE",
            validRequest.brandId(), validRequest.quantity(),
            validRequest.kmAtChange(), validRequest.intervalKm(),
            validRequest.airFilterChanged()
        );

        assertThrows(
            InvalidOilChangeDataException.class,
            () -> validationService.validateOilChangeRequest(request),
            "Debería fallar con tipo de aceite inválido"
        );
    }

    @Test
    public void testValidOilTypes() {
        // MOTOR debería pasar
        VehicleOilChangeRequest motorRequest = new VehicleOilChangeRequest(
            validRequest.placa(), validRequest.dateStamp(), "MOTOR",
            validRequest.brandId(), validRequest.quantity(),
            validRequest.kmAtChange(), validRequest.intervalKm(),
            validRequest.airFilterChanged()
        );
        assertDoesNotThrow(() -> validationService.validateOilChangeRequest(motorRequest));

        // HYDRAULIC debería pasar
        VehicleOilChangeRequest hydraulicRequest = new VehicleOilChangeRequest(
            validRequest.placa(), validRequest.dateStamp(), "HYDRAULIC",
            validRequest.brandId(), validRequest.quantity(),
            validRequest.kmAtChange(), validRequest.intervalKm(),
            validRequest.airFilterChanged()
        );
        assertDoesNotThrow(() -> validationService.validateOilChangeRequest(hydraulicRequest));

        // Minúsculas también deberían pasar
        VehicleOilChangeRequest lowercaseRequest = new VehicleOilChangeRequest(
            validRequest.placa(), validRequest.dateStamp(), "motor",
            validRequest.brandId(), validRequest.quantity(),
            validRequest.kmAtChange(), validRequest.intervalKm(),
            validRequest.airFilterChanged()
        );
        assertDoesNotThrow(() -> validationService.validateOilChangeRequest(lowercaseRequest));
    }

    @Test
    public void testEdgeCaseValues() {
        // Mínimos válidos
        VehicleOilChangeRequest minRequest = new VehicleOilChangeRequest(
            "ABC123", validRequest.dateStamp(), "MOTOR",
            validRequest.brandId(), 0.5, // Min quantity
            0, 100, // Min km, min interval
            false
        );
        assertDoesNotThrow(() -> validationService.validateOilChangeRequest(minRequest));

        // Máximos válidos
        VehicleOilChangeRequest maxRequest = new VehicleOilChangeRequest(
            "ABC123", validRequest.dateStamp(), "MOTOR",
            validRequest.brandId(), 100.0, // Max quantity
            999999, 100000, // Max km, max interval
            true
        );
        assertDoesNotThrow(() -> validationService.validateOilChangeRequest(maxRequest));
    }
}

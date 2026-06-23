package com.app.usochicamochabackend.shared.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OilChangeVehicleAlertCalculator Tests")
class OilChangeVehicleAlertCalculatorTest {

    @Test
    @DisplayName("Debe retornar VERDE para aceite en buen estado (< 70% uso)")
    void testGreenOilStatus() {
        // Último cambio: 200 km, Intervalo: 1000 km, Actual: 500 km
        // Porcentaje: (500-200)/1000*100 = 30%
        var result = OilChangeVehicleAlertCalculator.calculateOilChangeAlert(500, 200, 1000);

        assertEquals("VERDE", result.colorEstado);
        assertEquals(30.0, result.percentageUsed, 0.1);
        assertEquals(700, result.kmRemaining);  // 200 + 1000 - 500
    }

    @Test
    @DisplayName("Debe retornar AMARILLO para aceite con 70-90% de uso")
    void testYellowOilStatus() {
        // Último cambio: 200 km, Intervalo: 1000 km, Actual: 900 km
        // Porcentaje: (900-200)/1000*100 = 70%
        var result = OilChangeVehicleAlertCalculator.calculateOilChangeAlert(900, 200, 1000);

        assertEquals("AMARILLO", result.colorEstado);
        assertEquals(70.0, result.percentageUsed, 0.1);
        assertEquals(300, result.kmRemaining);  // 200 + 1000 - 900
    }

    @Test
    @DisplayName("Debe retornar ROJO para aceite con 90-100% de uso")
    void testRedOilStatus90Percent() {
        // Último cambio: 200 km, Intervalo: 1000 km, Actual: 1100 km
        // Porcentaje: (1100-200)/1000*100 = 90%
        var result = OilChangeVehicleAlertCalculator.calculateOilChangeAlert(1100, 200, 1000);

        assertEquals("ROJO", result.colorEstado);
        assertEquals(90.0, result.percentageUsed, 0.1);
        assertEquals(100, result.kmRemaining);  // 200 + 1000 - 1100
    }

    @Test
    @DisplayName("Debe retornar ROJO para aceite vencido (> 100% uso)")
    void testRedOilStatusExpired() {
        // Último cambio: 200 km, Intervalo: 1000 km, Actual: 1300 km
        // Porcentaje: (1300-200)/1000*100 = 110%
        var result = OilChangeVehicleAlertCalculator.calculateOilChangeAlert(1300, 200, 1000);

        assertEquals("ROJO", result.colorEstado);
        assertEquals(110.0, result.percentageUsed, 0.1);
        assertTrue(result.kmRemaining < 0);  // Ya pasó el cambio
    }

    @Test
    @DisplayName("Debe retornar VERDE cuando datos están incompletos")
    void testInvalidData() {
        var result = OilChangeVehicleAlertCalculator.calculateOilChangeAlert(null, 200, 1000);

        assertEquals("VERDE", result.colorEstado);
        assertEquals(0.0, result.percentageUsed);
    }

    @Test
    @DisplayName("Debe retornar VERDE para intervalo inválido o cero")
    void testZeroInterval() {
        var result = OilChangeVehicleAlertCalculator.calculateOilChangeAlert(500, 200, 0);

        assertEquals("VERDE", result.colorEstado);
    }
}

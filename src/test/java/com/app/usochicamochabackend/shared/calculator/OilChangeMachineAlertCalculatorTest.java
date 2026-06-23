package com.app.usochicamochabackend.shared.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OilChangeMachineAlertCalculator Tests")
class OilChangeMachineAlertCalculatorTest {

    @Test
    @DisplayName("Debe retornar VERDE para aceite motor en buen estado (< 70% uso)")
    void testGreenMotorOil() {
        // Último cambio: 100 hrs, Intervalo: 500 hrs, Actual: 250 hrs
        // Porcentaje: (250-100)/500*100 = 30%
        var result = OilChangeMachineAlertCalculator.calculateOilChangeAlert(250, 100, 500, "MOTOR");

        assertEquals("VERDE", result.colorEstado);
        assertEquals(30.0, result.percentageUsed, 0.1);
        assertEquals(350, result.horasRemaining);  // 100 + 500 - 250
    }

    @Test
    @DisplayName("Debe retornar AMARILLO para aceite hidráulico con 70-90% de uso")
    void testYellowHydraulicOil() {
        // Último cambio: 100 hrs, Intervalo: 500 hrs, Actual: 450 hrs
        // Porcentaje: (450-100)/500*100 = 70%
        var result = OilChangeMachineAlertCalculator.calculateOilChangeAlert(450, 100, 500, "HYDRAULIC");

        assertEquals("AMARILLO", result.colorEstado);
        assertEquals(70.0, result.percentageUsed, 0.1);
        assertEquals(150, result.horasRemaining);  // 100 + 500 - 450
        assertTrue(result.descripcion.contains("HIDRÁULICO"));
    }

    @Test
    @DisplayName("Debe retornar ROJO para aceite motor con 90% de uso")
    void testRedMotorOil90Percent() {
        // Último cambio: 100 hrs, Intervalo: 500 hrs, Actual: 550 hrs
        // Porcentaje: (550-100)/500*100 = 90%
        var result = OilChangeMachineAlertCalculator.calculateOilChangeAlert(550, 100, 500, "MOTOR");

        assertEquals("ROJO", result.colorEstado);
        assertEquals(90.0, result.percentageUsed, 0.1);
        assertEquals(50, result.horasRemaining);  // 100 + 500 - 550
        assertTrue(result.descripcion.contains("MOTOR"));
    }

    @Test
    @DisplayName("Debe retornar ROJO para aceite vencido (> 100% uso)")
    void testRedOilExpired() {
        // Último cambio: 100 hrs, Intervalo: 500 hrs, Actual: 650 hrs
        // Porcentaje: (650-100)/500*100 = 110%
        var result = OilChangeMachineAlertCalculator.calculateOilChangeAlert(650, 100, 500, "HYDRAULIC");

        assertEquals("ROJO", result.colorEstado);
        assertEquals(110.0, result.percentageUsed, 0.1);
        assertTrue(result.horasRemaining < 0);
    }

    @Test
    @DisplayName("Debe retornar VERDE con datos incompletos")
    void testInvalidData() {
        var result = OilChangeMachineAlertCalculator.calculateOilChangeAlert(null, 100, 500, "MOTOR");

        assertEquals("VERDE", result.colorEstado);
    }

    @Test
    @DisplayName("Debe retornar VERDE para intervalo inválido")
    void testZeroInterval() {
        var result = OilChangeMachineAlertCalculator.calculateOilChangeAlert(250, 100, 0, "HYDRAULIC");

        assertEquals("VERDE", result.colorEstado);
    }

    @Test
    @DisplayName("Debe distinguir entre MOTOR e HYDRAULIC en el mensaje")
    void testOilTypeLabels() {
        var motorResult = OilChangeMachineAlertCalculator.calculateOilChangeAlert(500, 100, 500, "MOTOR");
        var hydraulicResult = OilChangeMachineAlertCalculator.calculateOilChangeAlert(500, 100, 500, "HYDRAULIC");

        assertTrue(motorResult.descripcion.contains("MOTOR"));
        assertTrue(hydraulicResult.descripcion.contains("HIDRÁULICO"));
    }
}

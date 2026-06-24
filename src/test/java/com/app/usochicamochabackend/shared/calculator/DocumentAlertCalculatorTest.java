package com.app.usochicamochabackend.shared.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DocumentAlertCalculator Tests")
class DocumentAlertCalculatorTest {

    @Test
    @DisplayName("Debe retornar ROJO para documento vencido")
    void testVencidoDocument() {
        LocalDate vencido = LocalDate.now().minusDays(10);
        var result = DocumentAlertCalculator.calculateDocumentAlert("SOAT", vencido);

        assertEquals("ROJO", result.colorEstado);
        assertTrue(result.diasRestantes < 0);
    }

    @Test
    @DisplayName("Debe retornar ROJO para documento en la semana de vencimiento")
    void testUrgentDocument() {
        LocalDate urgente = LocalDate.now().plusDays(3);
        var result = DocumentAlertCalculator.calculateDocumentAlert("TECNOMECANICA", urgente);

        assertEquals("ROJO", result.colorEstado);
        assertTrue(result.diasRestantes > 0 && result.diasRestantes <= 7);
    }

    @Test
    @DisplayName("Debe retornar AMARILLO para documento próximo a vencer (30-45 días)")
    void testWarningDocument() {
        LocalDate proximo = LocalDate.now().plusDays(30);
        var result = DocumentAlertCalculator.calculateDocumentAlert("SOAT", proximo);

        assertEquals("AMARILLO", result.colorEstado);
        assertTrue(result.diasRestantes > 7 && result.diasRestantes <= 45);
    }

    @Test
    @DisplayName("Debe retornar VERDE para documento en buen estado")
    void testGreenDocument() {
        LocalDate bien = LocalDate.now().plusDays(60);
        var result = DocumentAlertCalculator.calculateDocumentAlert("RUNT", bien);

        assertEquals("VERDE", result.colorEstado);
        assertTrue(result.diasRestantes > 45);
    }

    @Test
    @DisplayName("EXTINTOR: Debe retornar ROJO si está vencido (mes actual > mes vencimiento)")
    void testExtintorVencido() {
        LocalDate extintorVencido = LocalDate.of(2025, 1, 15);  // Enero 2025
        var result = DocumentAlertCalculator.calculateDocumentAlert("EXTINTOR", extintorVencido);

        assertEquals("ROJO", result.colorEstado);
    }

    @Test
    @DisplayName("EXTINTOR: Debe retornar AMARILLO si vence el próximo mes")
    void testExtintorProximo() {
        LocalDate hoy = LocalDate.now();
        LocalDate extintorProximo = hoy.plusMonths(1);
        var result = DocumentAlertCalculator.calculateDocumentAlert("EXTINTOR", extintorProximo);

        // Un mes antes del vencimiento = AMARILLO según la lógica del calculador
        assertEquals("AMARILLO", result.colorEstado);
    }

    @Test
    @DisplayName("EXTINTOR: Debe retornar VERDE si vence en más de un mes")
    void testExtintorEnBuenEstado() {
        LocalDate hoy = LocalDate.now();
        LocalDate extintorLejos = hoy.plusMonths(3);
        var result = DocumentAlertCalculator.calculateDocumentAlert("EXTINTOR", extintorLejos);

        assertEquals("VERDE", result.colorEstado);
    }

    @Test
    @DisplayName("Debe retornar VERDE para documento null")
    void testNullDate() {
        var result = DocumentAlertCalculator.calculateDocumentAlert("SOAT", null);

        assertEquals("VERDE", result.colorEstado);
    }
}

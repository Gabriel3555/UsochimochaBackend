package com.app.usochicamochabackend.fuel.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Una fila de la proyección presupuestal (día 1 del mes, igual que
 * {@link FuelTrendResponse#mes()}, para que el frontend trate ambos igual) — histórica
 * ({@code proyectado=false}, viene de {@code obtenerTendencia}) o proyectada
 * ({@code proyectado=true}, mismo valor = promedio de los meses históricos cerrados
 * con actividad real).
 */
public record FuelBudgetProjectionRow(LocalDate mes, BigDecimal gastoNeto, boolean proyectado) {}

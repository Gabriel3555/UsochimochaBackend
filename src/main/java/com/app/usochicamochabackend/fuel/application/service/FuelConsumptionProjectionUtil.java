package com.app.usochicamochabackend.fuel.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Fórmula de proyección de consumo compartida entre {@link FuelPerformanceService}
 * (Rendimiento) y {@link FuelFullConsistencyService} (consistencia del "Full") —
 * antes vivía solo dentro de FuelPerformanceService, duplicarla ahí hubiera hecho
 * que ambos cálculos se desincronizaran con el tiempo.
 *
 * Las 4 unidades de consumo estándar (KM_POR_GALON, HORA_POR_GALON, KM_POR_M3,
 * HORA_POR_M3, ver V27) están en formato "rendimiento" — cuánto (km u horas) da
 * cada unidad de combustible — así que la fórmula es siempre la misma división,
 * sin ramas por unidad.
 */
final class FuelConsumptionProjectionUtil {

    private FuelConsumptionProjectionUtil() {
    }

    static BigDecimal proyectar(BigDecimal ejecutado, BigDecimal consumoEstandar) {
        return ejecutado.divide(consumoEstandar, 3, RoundingMode.HALF_UP);
    }
}

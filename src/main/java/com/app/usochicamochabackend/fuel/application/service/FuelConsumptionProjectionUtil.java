package com.app.usochicamochabackend.fuel.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Fórmula de proyección de consumo compartida entre {@link FuelPerformanceService}
 * (Rendimiento) y {@link FuelFullConsistencyService} (consistencia del "Full") —
 * antes vivía solo dentro de FuelPerformanceService, duplicarla ahí hubiera hecho
 * que ambos cálculos se desincronizaran con el tiempo.
 */
final class FuelConsumptionProjectionUtil {

    // Unidades con forma "tasa por hora" (GAL_POR_HORA, M3_POR_HORA): cantidad = horas × tasa.
    // Las de forma "distancia por unidad" (KM_POR_GALON, KM_POR_M3): cantidad = km / tasa.
    private static final String SUFIJO_POR_HORA = "_POR_HORA";

    private FuelConsumptionProjectionUtil() {
    }

    static BigDecimal proyectar(BigDecimal ejecutado, BigDecimal consumoEstandar, String unidadConsumo) {
        return unidadConsumo.endsWith(SUFIJO_POR_HORA)
                ? ejecutado.multiply(consumoEstandar)
                : ejecutado.divide(consumoEstandar, 3, RoundingMode.HALF_UP);
    }
}

package com.app.usochicamochabackend.update.application.service;

import com.app.usochicamochabackend.update.infrastructure.entity.VehicleOilChangeEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Calcula el estado del aceite y métricas asociadas
 * Reutilizable en VehicleMonitoringService, MotoMonitoringService y OilChangeService
 * Encapsula la lógica de thresholds y cálculos de estatus
 */
@Service
public class OilStatusCalculator {

    private static final int MIN_THRESHOLD = 100;

    public record OilStatus(
        String brandName,
        Double quantity,
        Integer intervalKm,
        LocalDate fechaUltimoCambio,
        Integer kmCambio,
        Integer kmProximoCambio,
        Integer kmParaProximo,
        Long diasDesdeUltimoCambio,
        Boolean filtroAire,
        String estado
    ) {}

    /**
     * Calcula el estado completo del aceite para vehículos/motos basado en:
     * - Kilómetros recorridos desde el último cambio
     * - Intervalo configurado
     * - Lógica de umbral adaptativo (20% del intervalo, mínimo 100 km)
     */
    public OilStatus calculate(VehicleOilChangeEntity change, Integer kmActual) {
        if (change == null) return null;

        Integer kmAtChange = change.getKmAtChange();
        Integer interval = change.getIntervalKm();
        Integer nextChangeKm = kmAtChange + interval;
        Integer kmRemaining = nextChangeKm - (kmActual != null ? kmActual : 0);

        Long diasDesdeUltimoCambio = ChronoUnit.DAYS.between(
            change.getDateStamp().toLocalDate(),
            LocalDate.now()
        );

        // Umbral adaptativo: 20% del intervalo con mínimo de 100 km
        // (Similar a machinery: 50 horas de ~10,000 hrs = 0.5% margen)
        int umbralProximo = Math.max(MIN_THRESHOLD, interval / 5);
        String estado = determineStatus(kmRemaining, umbralProximo);

        return new OilStatus(
            change.getBrand() != null ? change.getBrand().getName() : "N/A",
            change.getQuantity(),
            change.getIntervalKm(),
            change.getDateStamp().toLocalDate(),
            kmAtChange,
            nextChangeKm,
            kmRemaining,
            diasDesdeUltimoCambio,
            change.getAirFilterChanged() != null ? change.getAirFilterChanged() : false,
            estado
        );
    }

    /**
     * Determina el estado operativo del aceite basado en km restantes
     */
    private String determineStatus(int kmRemaining, int threshold) {
        if (kmRemaining > threshold) {
            return "OK";
        } else if (kmRemaining >= 0) {
            return "Proximo a cambio";
        } else {
            return "Cambio de Aceite";
        }
    }

    /**
     * Obtiene el umbral adaptativo para un intervalo dado
     * Útil para cálculos externos (ej: colores en frontend)
     */
    public int getThreshold(Integer intervalKm) {
        if (intervalKm == null || intervalKm <= 0) {
            intervalKm = 3000; // Default para motos
        }
        return Math.max(MIN_THRESHOLD, intervalKm / 5);
    }
}

package com.app.usochicamochabackend.fuel.application.service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Resuelve el rango de fechas de los reportes de fuel: si fechaInicio/fechaFin vienen
 * nulos, autogenera el mes actual (día 1 a hoy), igual que describe el BPMN pág. 3-4.
 * Compartido por los 4 reportes de Fase 2-3 para no repetir esta regla en cada Service.
 */
public final class FechaRangoUtil {

    private FechaRangoUtil() {}

    public record Rango(LocalDate fechaInicio, LocalDate fechaFin, Timestamp inicio, Timestamp fin) {}

    public static Rango resolver(LocalDate fechaInicio, LocalDate fechaFin) {
        LocalDate hoy = LocalDate.now();
        LocalDate inicio = fechaInicio != null ? fechaInicio : hoy.withDayOfMonth(1);
        LocalDate fin = fechaFin != null ? fechaFin : hoy;
        Timestamp inicioTs = Timestamp.valueOf(LocalDateTime.of(inicio, LocalTime.MIN));
        Timestamp finTs = Timestamp.valueOf(LocalDateTime.of(fin, LocalTime.MAX));
        return new Rango(inicio, fin, inicioTs, finTs);
    }
}

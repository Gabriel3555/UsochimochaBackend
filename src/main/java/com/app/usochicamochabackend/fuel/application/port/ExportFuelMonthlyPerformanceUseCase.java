package com.app.usochicamochabackend.fuel.application.port;

import java.time.LocalDate;

public interface ExportFuelMonthlyPerformanceUseCase {
    // fechaInicio/fechaFin nulos → todo el año actual hasta hoy (ver
    // FuelMonthlyPerformanceExcelExportService).
    byte[] exportarMensual(LocalDate fechaInicio, LocalDate fechaFin);
}

package com.app.usochicamochabackend.fuel.application.port;

import java.time.LocalDate;

public interface ExportRefuelingReportUseCase {
    byte[] exportarExcel(String tipo, String area, LocalDate fechaInicio, LocalDate fechaFin);
}

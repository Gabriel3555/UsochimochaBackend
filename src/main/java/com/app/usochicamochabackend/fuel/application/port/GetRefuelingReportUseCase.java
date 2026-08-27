package com.app.usochicamochabackend.fuel.application.port;

import com.app.usochicamochabackend.fuel.application.dto.RefuelingRecordResponse;

import java.time.LocalDate;
import java.util.List;

public interface GetRefuelingReportUseCase {
    List<RefuelingRecordResponse> obtenerReporte(String tipo, String area, LocalDate fechaInicio, LocalDate fechaFin);
}

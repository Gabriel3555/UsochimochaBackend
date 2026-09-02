package com.app.usochicamochabackend.fuel.application.port;

import com.app.usochicamochabackend.fuel.application.dto.RefuelingRecordResponse;

import java.time.LocalDate;
import java.util.List;

public interface GetRefuelingReportUseCase {
    List<RefuelingRecordResponse> obtenerReporte(String tipo, String area, LocalDate fechaInicio, LocalDate fechaFin);

    // Un solo tanqueo, con el mismo enriquecimiento (alertas, capacidad, etc.) que
    // obtenerReporte — para abrir el modal de editar sin traer todo el reporte.
    RefuelingRecordResponse obtenerPorId(Long id);
}

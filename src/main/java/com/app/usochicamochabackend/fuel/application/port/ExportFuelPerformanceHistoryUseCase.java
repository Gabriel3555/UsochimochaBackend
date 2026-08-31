package com.app.usochicamochabackend.fuel.application.port;

public interface ExportFuelPerformanceHistoryUseCase {
    // tipo: MAQUINARIA | VEHICULO | MOTOCICLETA (igual que GetFuelPerformanceUseCase);
    // activoId: machineId si tipo=MAQUINARIA, vehicleId en los otros dos casos.
    byte[] exportarHistorialActivo(String tipo, Long activoId);
}

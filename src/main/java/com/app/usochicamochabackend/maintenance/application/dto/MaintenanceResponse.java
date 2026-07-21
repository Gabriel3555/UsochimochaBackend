package com.app.usochicamochabackend.maintenance.application.dto;

import java.time.LocalDateTime;

public record MaintenanceResponse(
    Long id,
    LocalDateTime fecha,
    String placa,
    String ubicacion,
    String responsableAsignado,
    Integer kilometraje,
    String tipoMantenimiento,
    String repuestosMantenimiento,
    String tallerResponsable,
    String observaciones
) {}

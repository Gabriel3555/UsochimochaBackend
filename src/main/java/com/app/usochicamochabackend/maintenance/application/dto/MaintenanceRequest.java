package com.app.usochicamochabackend.maintenance.application.dto;

import java.time.LocalDateTime;

public record MaintenanceRequest(
    LocalDateTime fecha,
    Integer idVehiculo,
    Integer idUbicacion,
    String responsableAsignado,
    Integer kilometraje,
    String tipoMantenimiento,
    String repuestosMantenimiento,
    String tallerResponsable,
    String observaciones
) {}

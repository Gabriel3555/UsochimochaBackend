package com.app.usochicamochabackend.vehicle.application.service;

import com.app.usochicamochabackend.vehicleinspection.infrastructure.entity.DocumentacionYElementosEntity;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.repository.DocumentacionYElementosRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Calcula el estado de documentos (SOAT, TECNOMECANICA, etc.)
 * Reutilizable en VehicleMonitoringService, MotoMonitoringService y MachineryMonitoringService
 */
@Service
@RequiredArgsConstructor
public class DocumentStatusCalculator {

    private final DocumentacionYElementosRepository documentRepository;

    public record DocumentStatus(
        LocalDate fechaVencimiento,
        Long diasRestantes,
        String estado
    ) {}

    /**
     * Calcula el estado de un documento basado en días restantes hasta vencimiento
     * @param vehicleId ID del vehículo/máquina
     * @param type Tipo de documento (SOAT, TECNOMECANICA, etc.)
     * @return DocumentStatus con fecha, días restantes y estado (Vigente, Proximo a Vencer, Vencido)
     */
    public DocumentStatus calculate(Integer vehicleId, String type) {
        Optional<DocumentacionYElementosEntity> doc = documentRepository.findLatestByVehiculoAndTipo(vehicleId, type);

        if (doc.isEmpty()) return null;

        LocalDate expiry = doc.get().getFechaVencimiento();
        long days = ChronoUnit.DAYS.between(LocalDate.now(), expiry);
        String estado;

        if (days >= 15) {
            estado = "Vigente";
        } else if (days >= 0) {
            estado = "Proximo a Vencer";
        } else {
            estado = "Vencido";
        }

        return new DocumentStatus(expiry, days, estado);
    }
}

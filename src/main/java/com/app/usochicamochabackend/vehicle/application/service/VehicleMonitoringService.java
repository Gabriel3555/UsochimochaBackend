package com.app.usochicamochabackend.vehicle.application.service;

import com.app.usochicamochabackend.vehicle.application.dto.VehicleMonitoringDTO;
import com.app.usochicamochabackend.vehicle.application.port.VehicleMonitoringUseCase;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.VehicleEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleRepository;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.entity.DocumentacionYElementosEntity;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.entity.InspPreOperativaEntity;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.repository.DocumentacionYElementosRepository;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.repository.InspPreOperativaRepository;
import com.app.usochicamochabackend.update.infrastructure.entity.VehicleOilChangeEntity;
import com.app.usochicamochabackend.update.infrastructure.repository.VehicleOilChangeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VehicleMonitoringService implements VehicleMonitoringUseCase {

    private final VehicleRepository vehicleRepository;
    private final InspPreOperativaRepository inspectionRepository;
    private final DocumentacionYElementosRepository documentRepository;
    private final VehicleOilChangeRepository oilChangeRepository;

    @Override
    public List<VehicleMonitoringDTO> getConsolidatedMonitoring() {
        List<VehicleEntity> vehicles = vehicleRepository.findAllByTipoNameNot("MOTOCICLETA");
        List<VehicleMonitoringDTO> result = new ArrayList<>();

        for (VehicleEntity vehicle : vehicles) {
            result.add(buildMonitoringDTO(vehicle));
        }

        return result;
    }

    private VehicleMonitoringDTO buildMonitoringDTO(VehicleEntity vehicle) {
        Optional<InspPreOperativaEntity> lastInsp = inspectionRepository.findLatestByVehicleId(vehicle.getIdVehiculo());

        Long daysSinceReport = null;
        java.time.LocalDateTime lastReportDate = vehicle.getFechaUltimoReporte();

        // Si no está en el campo de la entidad, obtenerlo de la última inspección
        if (lastReportDate == null && lastInsp.isPresent()) {
            lastReportDate = lastInsp.get().getFechaRegistro();
        }

        // Calcular días desde reporte si hay fecha disponible
        if (lastReportDate != null) {
            daysSinceReport = ChronoUnit.DAYS.between(lastReportDate.toLocalDate(), LocalDate.now());
        }

        return new VehicleMonitoringDTO(
            vehicle.getPlaca(),
            vehicle.getBelongsTo(),
            vehicle.getKilometrajeActual(),
            lastReportDate,
            daysSinceReport,
            getDocumentStatus(vehicle.getIdVehiculo(), "SOAT"),
            getDocumentStatus(vehicle.getIdVehiculo(), "TECNOMECANICA"),
            getOilStatus(vehicle)
        );
    }

    private VehicleMonitoringDTO.DocumentStatus getDocumentStatus(Integer vehicleId, String type) {
        Optional<DocumentacionYElementosEntity> doc = documentRepository.findLatestByVehiculoAndTipo(vehicleId, type);
        
        if (doc.isEmpty()) return null;

        LocalDate expiry = doc.get().getFechaVencimiento();
        long days = ChronoUnit.DAYS.between(LocalDate.now(), expiry);
        String estado;

        if (days >= 15) {
            estado = "Vigente";
        } else if (days >= 0) {
            estado = "Próximo a Vencer";
        } else {
            estado = "Vencido";
        }

        return new VehicleMonitoringDTO.DocumentStatus(expiry, days, estado);
    }

    private VehicleMonitoringDTO.OilStatus getOilStatus(VehicleEntity vehicle) {
        Optional<VehicleOilChangeEntity> lastChange = oilChangeRepository.findLatestByVehicleId(vehicle.getIdVehiculo());

        if (lastChange.isEmpty()) return null;

        VehicleOilChangeEntity change = lastChange.get();
        Integer kmAtChange = change.getKmAtChange();
        Integer interval = change.getIntervalKm();
        Integer nextChangeKm = kmAtChange + interval;
        Integer kmActual = vehicle.getKilometrajeActual() != null ? vehicle.getKilometrajeActual() : 0;
        Integer kmRemaining = nextChangeKm - kmActual;

        String estado;
        if (kmRemaining > 500) {
            estado = "OK";
        } else if (kmRemaining >= 0) {
            estado = "Próximo a cambio";
        } else {
            estado = "Cambio de Aceite";
        }

        return new VehicleMonitoringDTO.OilStatus(
            change.getOilType(),
            change.getDateStamp().toLocalDate(),
            kmAtChange,
            nextChangeKm,
            kmRemaining,
            estado
        );
    }
}

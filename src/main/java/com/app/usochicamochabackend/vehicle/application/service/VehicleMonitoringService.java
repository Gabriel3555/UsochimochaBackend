package com.app.usochicamochabackend.vehicle.application.service;

import com.app.usochicamochabackend.shared.calculator.OilChangeAlertCalculator;
import com.app.usochicamochabackend.shared.dto.AlertStatus;
import com.app.usochicamochabackend.vehicle.application.dto.VehicleMonitoringDTO;
import com.app.usochicamochabackend.vehicle.application.port.VehicleMonitoringUseCase;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.VehicleEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleRepository;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.entity.InspPreOperativaEntity;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.repository.InspPreOperativaRepository;
import com.app.usochicamochabackend.update.infrastructure.repository.VehicleOilChangeRepository;
import com.app.usochicamochabackend.update.application.service.OilStatusCalculator;
import com.app.usochicamochabackend.notifications.application.ImprovedOilChangeAlertService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VehicleMonitoringService implements VehicleMonitoringUseCase {

    private static final Logger logger = LoggerFactory.getLogger(VehicleMonitoringService.class);

    private final VehicleRepository vehicleRepository;
    private final InspPreOperativaRepository inspectionRepository;
    private final VehicleOilChangeRepository oilChangeRepository;
    private final DocumentStatusCalculator documentStatusCalculator;
    private final OilStatusCalculator oilStatusCalculator;
    private final ImprovedOilChangeAlertService improvedOilChangeAlertService;

    @Override
    public List<VehicleMonitoringDTO> getConsolidatedMonitoring() {
        logger.info("Iniciando obtención de monitoreo consolidado de vehículos");
        long startTime = System.currentTimeMillis();

        try {
            // Calcular alertas de cambio de aceite para vehículos
            try {
                improvedOilChangeAlertService.calculateAllOilChangeAlerts();
            } catch (Exception e) {
                logger.warn("⚠️ Error calculando alertas de cambio de aceite al cargar consolidado: {}", e.getMessage());
            }

            // Query optimizada con eager loading para evitar N+1
            List<VehicleEntity> vehicles = vehicleRepository.findAllByTipoNameNot("MOTOCICLETA");
            logger.debug("Se obtuvieron {} vehículos (sin motos)", vehicles.size());

            List<VehicleMonitoringDTO> result = new ArrayList<>();

            for (VehicleEntity vehicle : vehicles) {
                result.add(buildMonitoringDTO(vehicle));
            }

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Monitoreo consolidado completado: {} vehículos en {} ms",
                result.size(), duration);

            return result;

        } catch (Exception e) {
            logger.error("Error al obtener monitoreo consolidado de vehículos", e);
            throw e;
        }
    }

    private VehicleMonitoringDTO buildMonitoringDTO(VehicleEntity vehicle) {
        Optional<InspPreOperativaEntity> lastInsp = inspectionRepository.findLatestByVehicleId(vehicle.getIdVehiculo());

        Long daysSinceReport = null;
        java.time.LocalDateTime lastReportDate = vehicle.getFechaUltimoReporte();

        if (lastReportDate == null && lastInsp.isPresent()) {
            lastReportDate = lastInsp.get().getFechaRegistro();
        }

        if (lastReportDate != null) {
            daysSinceReport = ChronoUnit.DAYS.between(lastReportDate.toLocalDate(), LocalDate.now());
        }

        return new VehicleMonitoringDTO(
            vehicle.getPlaca(),
            vehicle.getBelongsTo(),
            vehicle.getKilometrajeActual(),
            lastReportDate,
            daysSinceReport,
            toVehicleDocumentStatus(documentStatusCalculator.calculate(vehicle.getIdVehiculo(), "SOAT")),
            toVehicleDocumentStatus(documentStatusCalculator.calculate(vehicle.getIdVehiculo(), "TECNOMECANICA")),
            getOilStatus(vehicle)
        );
    }

    private VehicleMonitoringDTO.DocumentStatus toVehicleDocumentStatus(DocumentStatusCalculator.DocumentStatus ds) {
        if (ds == null) return null;
        return new VehicleMonitoringDTO.DocumentStatus(ds.fechaVencimiento(), ds.diasRestantes(), ds.estado());
    }

    private VehicleMonitoringDTO.OilStatus getOilStatus(VehicleEntity vehicle) {
        var lastChange = oilChangeRepository.findFirstByVehicleIdVehiculoOrderByDateStampDesc(vehicle.getIdVehiculo());

        if (lastChange.isEmpty()) {
            logger.debug("Sin historial de cambio de aceite para vehículo: {}", vehicle.getPlaca());
            return null;
        }

        var oilStatus = oilStatusCalculator.calculate(lastChange.get(), vehicle.getKilometrajeActual());

        if (oilStatus == null) return null;

        // Cálculo unificado de alerta usando OilChangeAlertCalculator
        long distanceSinceChange = vehicle.getKilometrajeActual() - oilStatus.kmCambio();
        AlertStatus alertStatus = OilChangeAlertCalculator.calculateAlert(
            distanceSinceChange,
            oilStatus.intervalKm().longValue()
        );

        return new VehicleMonitoringDTO.OilStatus(
            oilStatus.brandName(),
            oilStatus.quantity(),
            oilStatus.intervalKm(),
            oilStatus.fechaUltimoCambio(),
            oilStatus.kmCambio(),
            oilStatus.kmProximoCambio(),
            oilStatus.kmParaProximo(),
            oilStatus.diasDesdeUltimoCambio(),
            oilStatus.filtroAire(),
            oilStatus.estado(),
            alertStatus.percentageUsed(),      // % de uso
            alertStatus.color(),                // Color (RED, YELLOW, BLUE, GREEN)
            alertStatus.message()               // Mensaje de alerta
        );
    }
}

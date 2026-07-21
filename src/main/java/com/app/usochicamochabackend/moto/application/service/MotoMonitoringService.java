package com.app.usochicamochabackend.moto.application.service;

import com.app.usochicamochabackend.catalog.infrastructure.entity.UbicacionEntity;
import com.app.usochicamochabackend.catalog.infrastructure.repository.UbicacionRepository;
import com.app.usochicamochabackend.moto.application.dto.MotoMonitoringDTO;
import com.app.usochicamochabackend.moto.application.port.MotoMonitoringUseCase;
import com.app.usochicamochabackend.shared.calculator.OilChangeAlertCalculator;
import com.app.usochicamochabackend.shared.dto.AlertStatus;
import com.app.usochicamochabackend.vehicle.application.service.DocumentStatusCalculator;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.VehicleEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleRepository;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.entity.InspPreOperativaEntity;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.repository.InspPreOperativaRepository;
import com.app.usochicamochabackend.update.infrastructure.repository.VehicleOilChangeRepository;
import com.app.usochicamochabackend.update.application.service.OilStatusCalculator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MotoMonitoringService implements MotoMonitoringUseCase {

    private static final Logger logger = LoggerFactory.getLogger(MotoMonitoringService.class);

    private final VehicleRepository vehicleRepository;
    private final InspPreOperativaRepository inspectionRepository;
    private final VehicleOilChangeRepository oilChangeRepository;
    private final UbicacionRepository ubicacionRepository;
    private final DocumentStatusCalculator documentStatusCalculator;
    private final OilStatusCalculator oilStatusCalculator;

    @Override
    public List<MotoMonitoringDTO> getConsolidatedMonitoring() {
        logger.info("Iniciando obtención de monitoreo consolidado de motocicletas");
        long startTime = System.currentTimeMillis();

        try {
            List<VehicleEntity> motos = vehicleRepository.findAllByTipoName("MOTOCICLETA");
            logger.debug("Se obtuvieron {} motocicletas", motos.size());

            List<MotoMonitoringDTO> result = new ArrayList<>();

            for (VehicleEntity moto : motos) {
                result.add(buildMonitoringDTO(moto));
            }

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Monitoreo consolidado completado: {} motocicletas en {} ms",
                result.size(), duration);

            return result;

        } catch (Exception e) {
            logger.error("Error al obtener monitoreo consolidado de motocicletas", e);
            throw e;
        }
    }

    private MotoMonitoringDTO buildMonitoringDTO(VehicleEntity moto) {
        Optional<InspPreOperativaEntity> lastInsp = inspectionRepository.findLatestByVehicleId(moto.getIdVehiculo());

        Long daysSinceReport = null;
        LocalDateTime lastReportDate = moto.getFechaUltimoReporte();
        String estadoMoto = "Optimo";
        String novedad = "Ninguna";
        String unidadUltimoReporte = null;

        if (lastInsp.isPresent()) {
            InspPreOperativaEntity insp = lastInsp.get();
            lastReportDate = insp.getFechaRegistro();
            daysSinceReport = ChronoUnit.DAYS.between(lastReportDate.toLocalDate(), LocalDate.now());
            estadoMoto = insp.getEstadoVehiculo() != null ? insp.getEstadoVehiculo() : "Regular";
            novedad = (insp.getObservacionesFinales() != null && !insp.getObservacionesFinales().isBlank())
                      ? insp.getObservacionesFinales() : "Ninguna";
            Integer idUb = insp.getIdUbicacion();
            if (idUb != null) {
                unidadUltimoReporte = ubicacionRepository.findById(idUb)
                        .map(UbicacionEntity::getNombreUbicacion)
                        .orElse(null);
            }
        } else if (lastReportDate != null) {
            daysSinceReport = ChronoUnit.DAYS.between(lastReportDate.toLocalDate(), LocalDate.now());
        }

        String ubicacionCatalogo = null;
        if (moto.getUbicacionBase() != null) {
            ubicacionCatalogo = moto.getUbicacionBase().getNombreUbicacion();
        }

        return new MotoMonitoringDTO(
            moto.getBelongsTo(),
            ubicacionCatalogo,
            unidadUltimoReporte,
            moto.getPlaca(),
            moto.getKilometrajeActual(),
            toMotoDocumentStatus(documentStatusCalculator.calculate(moto.getIdVehiculo(), "SOAT")),
            toMotoDocumentStatus(documentStatusCalculator.calculate(moto.getIdVehiculo(), "TECNOMECANICA")),
            getOilStatus(moto),
            estadoMoto,
            lastReportDate,
            daysSinceReport,
            novedad
        );
    }

    private MotoMonitoringDTO.DocumentStatus toMotoDocumentStatus(DocumentStatusCalculator.DocumentStatus ds) {
        if (ds == null) return null;
        return new MotoMonitoringDTO.DocumentStatus(ds.fechaVencimiento(), ds.diasRestantes(), ds.estado());
    }

    private MotoMonitoringDTO.OilStatus getOilStatus(VehicleEntity moto) {
        var lastChange = oilChangeRepository.findFirstByVehicleIdVehiculoOrderByDateStampDesc(moto.getIdVehiculo());
        if (lastChange.isEmpty()) {
            logger.debug("Sin historial de cambio de aceite para motocicleta: {}", moto.getPlaca());
            return null;
        }

        var oilStatus = oilStatusCalculator.calculate(lastChange.get(), moto.getKilometrajeActual());

        if (oilStatus == null) return null;

        // Cálculo unificado de alerta usando OilChangeAlertCalculator
        long distanceSinceChange = moto.getKilometrajeActual() - oilStatus.kmCambio();
        AlertStatus alertStatus = OilChangeAlertCalculator.calculateAlert(
            distanceSinceChange,
            oilStatus.intervalKm().longValue()
        );

        return new MotoMonitoringDTO.OilStatus(
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

package com.app.usochicamochabackend.moto.application.service;

import com.app.usochicamochabackend.catalog.infrastructure.entity.UbicacionEntity;
import com.app.usochicamochabackend.catalog.infrastructure.repository.UbicacionRepository;
import com.app.usochicamochabackend.moto.application.dto.MotoMonitoringDTO;
import com.app.usochicamochabackend.moto.application.port.MotoMonitoringUseCase;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MotoMonitoringService implements MotoMonitoringUseCase {

    private final VehicleRepository vehicleRepository;
    private final InspPreOperativaRepository inspectionRepository;
    private final DocumentacionYElementosRepository documentRepository;
    private final VehicleOilChangeRepository oilChangeRepository;
    private final UbicacionRepository ubicacionRepository;

    @Override
    public List<MotoMonitoringDTO> getConsolidatedMonitoring() {
        List<VehicleEntity> motos = vehicleRepository.findAllByTipoName("MOTOCICLETA");
        List<MotoMonitoringDTO> result = new ArrayList<>();

        for (VehicleEntity moto : motos) {
            result.add(buildMonitoringDTO(moto));
        }

        return result;
    }

    private MotoMonitoringDTO buildMonitoringDTO(VehicleEntity moto) {
        Optional<InspPreOperativaEntity> lastInsp = inspectionRepository.findLatestByVehicleId(moto.getIdVehiculo());

        Long daysSinceReport = null;
        LocalDateTime lastReportDate = moto.getFechaUltimoReporte();
        String estadoMoto = "Óptimo";
        String novedad = "Ninguna";
        /** Excel «Responsable / unidad»: nombre de estación donde se reportó la última inspección (app móvil envía idUbicacion). */
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
            // Si no hay inspección pero sí hay fecha en la entidad, calcular días
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
            getDocumentStatus(moto.getIdVehiculo(), "SOAT"),
            getDocumentStatus(moto.getIdVehiculo(), "TECNOMECANICA"),
            getOilStatus(moto),
            estadoMoto,
            lastReportDate,
            daysSinceReport,
            novedad
        );
    }

    private MotoMonitoringDTO.DocumentStatus getDocumentStatus(Integer vehicleId, String type) {
        Optional<DocumentacionYElementosEntity> doc = documentRepository.findLatestByVehiculoAndTipo(vehicleId, type);
        if (doc.isEmpty()) return null;

        LocalDate expiry = doc.get().getFechaVencimiento();
        long days = ChronoUnit.DAYS.between(LocalDate.now(), expiry);
        String estado = (days >= 15) ? "Vigente" : (days >= 0 ? "Próximo a Vencer" : "Vencido");

        return new MotoMonitoringDTO.DocumentStatus(expiry, days, estado);
    }

    private MotoMonitoringDTO.OilStatus getOilStatus(VehicleEntity moto) {
        Optional<VehicleOilChangeEntity> lastChange = oilChangeRepository.findLatestByVehicleId(moto.getIdVehiculo());
        if (lastChange.isEmpty()) return null;

        VehicleOilChangeEntity change = lastChange.get();
        Integer kmAtChange = change.getKmAtChange();
        Integer nextChangeKm = kmAtChange + change.getIntervalKm();
        Integer kmActual = moto.getKilometrajeActual() != null ? moto.getKilometrajeActual() : 0;
        Integer kmRemaining = nextChangeKm - kmActual;

        // Motos: intervalos cortos; “próximo” cuando quedan ≤ ~20 % del intervalo (mín. 100 km).
        int interval = change.getIntervalKm() != null && change.getIntervalKm() > 0 ? change.getIntervalKm() : 3000;
        int umbralProximo = Math.max(100, interval / 5);
        String estado = (kmRemaining > umbralProximo) ? "OK" : (kmRemaining >= 0 ? "Próximo a Cambio" : "Cambio de Aceite");

        return new MotoMonitoringDTO.OilStatus(
            change.getDateStamp().toLocalDate(),
            kmAtChange,
            nextChangeKm,
            kmRemaining,
            change.getAirFilterChanged(),
            estado
        );
    }
}

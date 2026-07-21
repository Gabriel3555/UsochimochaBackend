package com.app.usochicamochabackend.vehicleinspection.application.port;

import com.app.usochicamochabackend.vehicleinspection.application.dto.VehicleInspectionReportDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface GetVehicleInspectionsUseCase {
    List<VehicleInspectionReportDTO> getInspectionsByType(Integer typeId);

    /** Todas las inspecciones de vehículos (excluye motos), más recientes primero. */
    List<VehicleInspectionReportDTO> getAllVehicleInspections();

    /** Una fila por placa, excluyendo motos (la más reciente por vehículo). */
    List<VehicleInspectionReportDTO> getLatestVehicleInspectionsPerVehicle();

    /** Una fila por placa: inspección con {@code fecha_registro} más reciente (evita duplicados por varios {@code id_vehiculo}). */
    List<VehicleInspectionReportDTO> getMotoInspectionsLatestPerVehicle();

    /** Todas las inspecciones de motos, más recientes primero (historial). */
    List<VehicleInspectionReportDTO> getMotoInspectionsHistory();

    /** Todas las inspecciones de motos paginadas. */
    Page<VehicleInspectionReportDTO> getMotoInspectionsPaginated(Pageable pageable);
}

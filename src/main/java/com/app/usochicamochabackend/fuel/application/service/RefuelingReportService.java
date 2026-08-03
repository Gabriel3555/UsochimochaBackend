package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.fuel.application.dto.RefuelingRecordResponse;
import com.app.usochicamochabackend.fuel.application.port.GetRefuelingReportUseCase;
import com.app.usochicamochabackend.fuel.infrastructure.entity.RefuelingRecordsEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.RefuelingRecordsRepository;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.VehicleEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Reporte plano (sin agregar) de tanqueos por tipo de activo + rango de fechas +
 * área de costo opcional. Reemplaza, del lado del reporte "Tanqueo y Distribución",
 * el agrupado por `lugar` de {@link FuelDistributionService} — en la operación real
 * BOMBA=Vehículos y ALMACEN=Maquinaria+Motocicletas siempre, así que agrupar por
 * tipo de activo es la misma partición con mejor lectura para el usuario.
 */
@Service
@RequiredArgsConstructor
public class RefuelingReportService implements GetRefuelingReportUseCase {

    private static final String VEHICULO = "VEHICULO";
    private static final String MAQUINARIA_MOTO = "MAQUINARIA_MOTO";
    private static final String MOTOCICLETA = "MOTOCICLETA";
    private static final String AREA_TODAS = "TODAS";

    private final RefuelingRecordsRepository refuelingRecordsRepository;
    private final VehicleRepository vehicleRepository;
    private final AssetFuelCapacityService assetFuelCapacityService;

    @Override
    public List<RefuelingRecordResponse> obtenerReporte(String tipo, String area, LocalDate fechaInicio, LocalDate fechaFin) {
        if (!VEHICULO.equals(tipo) && !MAQUINARIA_MOTO.equals(tipo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tipo debe ser VEHICULO o MAQUINARIA_MOTO.");
        }
        FechaRangoUtil.Rango rango = FechaRangoUtil.resolver(fechaInicio, fechaFin);

        List<RefuelingRecordsEntity> tanqueos;
        if (VEHICULO.equals(tipo)) {
            tanqueos = filtrarPorTipoVehiculo(
                    refuelingRecordsRepository.findByVehicleIdIsNotNullAndFechaRegistroBetween(rango.inicio(), rango.fin()),
                    false);
        } else {
            List<RefuelingRecordsEntity> maquinas =
                    refuelingRecordsRepository.findByMachineIdIsNotNullAndFechaRegistroBetween(rango.inicio(), rango.fin());
            List<RefuelingRecordsEntity> motos = filtrarPorTipoVehiculo(
                    refuelingRecordsRepository.findByVehicleIdIsNotNullAndFechaRegistroBetween(rango.inicio(), rango.fin()),
                    true);
            tanqueos = new ArrayList<>(maquinas.size() + motos.size());
            tanqueos.addAll(maquinas);
            tanqueos.addAll(motos);
        }

        boolean todas = area == null || AREA_TODAS.equals(area);
        if (!todas) {
            tanqueos = tanqueos.stream().filter(t -> area.equals(t.getAreaCosto())).toList();
        }

        return tanqueos.stream()
                .map(t -> RefuelingRecordResponse.from(t,
                        assetFuelCapacityService.excedeCapacidad(t.getVehicleId(), t.getMachineId(), t.getCantidadGalones())))
                .toList();
    }

    // Mismo patrón que FuelPerformanceService.filtrarPorTipoVehiculo, simplificado a
    // un booleano porque aquí solo hay 2 categorías (moto sí/no) en vez de 3 tipos.
    private List<RefuelingRecordsEntity> filtrarPorTipoVehiculo(List<RefuelingRecordsEntity> tanqueos, boolean esMoto) {
        return tanqueos.stream()
                .filter(t -> vehicleRepository.findById(t.getVehicleId())
                        .map(VehicleEntity::getTipoVehiculo)
                        .map(tv -> esMoto == MOTOCICLETA.equalsIgnoreCase(tv.getNombreTipo()))
                        .orElse(false))
                .toList();
    }
}

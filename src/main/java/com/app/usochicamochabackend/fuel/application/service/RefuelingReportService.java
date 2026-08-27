package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.fuel.application.dto.RefuelingRecordResponse;
import com.app.usochicamochabackend.fuel.application.port.GetRefuelingReportUseCase;
import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelReintegrationsEntity;
import com.app.usochicamochabackend.fuel.infrastructure.entity.RefuelingRecordsEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelReintegrationsRepository;
import com.app.usochicamochabackend.fuel.infrastructure.repository.RefuelingRecordsRepository;
import com.app.usochicamochabackend.machine.infrastructure.entity.MachineEntity;
import com.app.usochicamochabackend.machine.infrastructure.repository.MachineRepository;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.VehicleEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Reporte plano (sin agregar) de tanqueos por rango de fechas + área de costo
 * opcional. Reemplaza, del lado del reporte "Tanqueo y Distribución", el agrupado
 * por `lugar` de {@link FuelDistributionService}.
 *
 * "tipo" agrupa por el `lugar` REAL del tanqueo (BOMBA/ALMACEN), no por el tipo de
 * activo — antes agrupaba fijo (Vehículos siempre en "Estación", Maquinaria+Motos
 * siempre en "Almacén" sin importar dónde se tanqueó), lo que confundía cuando una
 * moto se tanqueaba de verdad en una bomba/estación y el usuario esperaba verla ahí.
 * El formulario de registro ahora sugiere el lugar típico por tipo de activo (moto/
 * máquina → ALMACEN, vehículo → BOMBA) pero sigue siendo editable, así que este
 * reporte refleja la excepción real en vez de ocultarla.
 */
@Service
@RequiredArgsConstructor
public class RefuelingReportService implements GetRefuelingReportUseCase {

    private static final String VEHICULO = "VEHICULO";
    private static final String MAQUINARIA_MOTO = "MAQUINARIA_MOTO";
    private static final String AREA_TODAS = "TODAS";

    private final RefuelingRecordsRepository refuelingRecordsRepository;
    private final AssetFuelCapacityService assetFuelCapacityService;
    private final FuelPriceAnomalyService fuelPriceAnomalyService;
    private final FuelFullConsistencyService fuelFullConsistencyService;
    private final FuelReintegrationsRepository fuelReintegrationsRepository;
    private final VehicleRepository vehicleRepository;
    private final MachineRepository machineRepository;

    @Override
    public List<RefuelingRecordResponse> obtenerReporte(String tipo, String area, LocalDate fechaInicio, LocalDate fechaFin) {
        if (!VEHICULO.equals(tipo) && !MAQUINARIA_MOTO.equals(tipo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tipo debe ser VEHICULO o MAQUINARIA_MOTO.");
        }
        FechaRangoUtil.Rango rango = FechaRangoUtil.resolver(fechaInicio, fechaFin);

        String lugar = VEHICULO.equals(tipo) ? "BOMBA" : "ALMACEN";
        List<RefuelingRecordsEntity> tanqueos =
                refuelingRecordsRepository.findByLugarAndFechaRegistroBetween(lugar, rango.inicio(), rango.fin());

        boolean todas = area == null || AREA_TODAS.equals(area);
        if (!todas) {
            tanqueos = tanqueos.stream().filter(t -> area.equals(t.getAreaCosto())).toList();
        }

        return tanqueos.stream().map(this::mapToResponse).toList();
    }

    private RefuelingRecordResponse mapToResponse(RefuelingRecordsEntity t) {
        BigDecimal capacidadConfiguradaGal = assetFuelCapacityService.capacidadConfigurada(t.getVehicleId(), t.getMachineId());
        String assetName = resolverNombreActivo(t.getVehicleId(), t.getMachineId());
        String assetType = resolverTipoActivo(t.getVehicleId(), t.getMachineId());

        return RefuelingRecordResponse.from(t,
                assetFuelCapacityService.excedeCapacidad(t.getVehicleId(), t.getMachineId(), t.getCantidadGalones()),
                assetFuelCapacityService.cantidadFueraDeRangoTipico(t.getVehicleId(), t.getMachineId(), t.getCantidadGalones()),
                fuelPriceAnomalyService.precioFueraDeRango(t.getFuelTypeId(), t.getPrecioUnitario(), t.getId()),
                fuelFullConsistencyService.fullInconsistente(t.getVehicleId(), t.getMachineId(), t.getEsFull(),
                        t.getCantidadGalones(), t.getHorometroKm(), t.getFechaRegistro(), capacidadConfiguradaGal),
                sumaReintegros(t.getId()),
                capacidadConfiguradaGal,
                assetFuelCapacityService.maximoTipico(t.getVehicleId(), t.getMachineId()),
                fuelPriceAnomalyService.promedioReciente(t.getFuelTypeId(), t.getId()),
                fuelFullConsistencyService.cantidadEsperadaParaLleno(t.getVehicleId(), t.getMachineId(), t.getEsFull(),
                        t.getHorometroKm(), t.getFechaRegistro(), capacidadConfiguradaGal),
                assetName,
                assetType);
    }

    private String resolverNombreActivo(Integer vehicleId, Long machineId) {
        if (machineId != null) {
            Optional<MachineEntity> machine = machineRepository.findById(machineId);
            if (machine.isPresent()) {
                return machine.get().getName();
            }
            return "Máquina #" + machineId;
        }
        if (vehicleId != null) {
            Optional<VehicleEntity> vehicle = vehicleRepository.findById(vehicleId);
            if (vehicle.isPresent()) {
                return vehicle.get().getPlaca();
            }
            return "Vehículo #" + vehicleId;
        }
        return "Desconocido";
    }

    private String resolverTipoActivo(Integer vehicleId, Long machineId) {
        if (machineId != null) {
            return "Máquina";
        }
        if (vehicleId != null) {
            Optional<VehicleEntity> vehicle = vehicleRepository.findById(vehicleId);
            if (vehicle.isPresent()) {
                String tipoVehiculo = vehicle.get().getTipoVehiculo() != null ?
                    vehicle.get().getTipoVehiculo().getNombreTipo() : "VEHICULO";
                if ("MOTOCICLETA".equalsIgnoreCase(tipoVehiculo)) {
                    return "Motocicleta";
                }
            }
            return "Vehículo";
        }
        return "Desconocido";
    }

    private BigDecimal sumaReintegros(Long refuelingId) {
        return fuelReintegrationsRepository.findByRefuelingId(refuelingId).stream()
                .map(FuelReintegrationsEntity::getCantidadReintegrada)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

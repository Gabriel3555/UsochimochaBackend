package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.fuel.application.dto.FuelPerformanceResponse;
import com.app.usochicamochabackend.fuel.application.port.GetFuelPerformanceUseCase;
import com.app.usochicamochabackend.fuel.infrastructure.entity.AssetFuelConfigEntity;
import com.app.usochicamochabackend.fuel.infrastructure.entity.RefuelingRecordsEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.AssetFuelConfigRepository;
import com.app.usochicamochabackend.fuel.infrastructure.repository.RefuelingRecordsRepository;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.VehicleEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FuelPerformanceService implements GetFuelPerformanceUseCase {

    private static final String MAQUINARIA = "MAQUINARIA";
    private static final String VEHICULO = "VEHICULO";
    private static final String MOTOCICLETA = "MOTOCICLETA";
    // Unidades con forma "tasa por hora" (GAL_POR_HORA, M3_POR_HORA): cantidad = horas × tasa.
    // Las de forma "distancia por unidad" (KM_POR_GALON, KM_POR_M3): cantidad = km / tasa.
    private static final String SUFIJO_POR_HORA = "_POR_HORA";

    private final RefuelingRecordsRepository refuelingRecordsRepository;
    private final AssetFuelConfigRepository assetFuelConfigRepository;
    private final VehicleRepository vehicleRepository;

    @Value("${app.fuel.rendimiento-desviacion-tolerancia-porcentaje:0.15}")
    private BigDecimal tolerancia;

    @Override
    public List<FuelPerformanceResponse> obtenerRendimiento(String tipo, LocalDate fechaInicio, LocalDate fechaFin) {
        if (!MAQUINARIA.equals(tipo) && !VEHICULO.equals(tipo) && !MOTOCICLETA.equals(tipo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "tipo debe ser MAQUINARIA, VEHICULO o MOTOCICLETA.");
        }
        FechaRangoUtil.Rango rango = FechaRangoUtil.resolver(fechaInicio, fechaFin);

        List<RefuelingRecordsEntity> tanqueos = MAQUINARIA.equals(tipo)
                ? refuelingRecordsRepository.findByMachineIdIsNotNullAndFechaRegistroBetween(rango.inicio(), rango.fin())
                : filtrarPorTipoVehiculo(
                        refuelingRecordsRepository.findByVehicleIdIsNotNullAndFechaRegistroBetween(rango.inicio(), rango.fin()),
                        tipo);

        return tanqueos.stream()
                .map(this::calcularFila)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    private List<RefuelingRecordsEntity> filtrarPorTipoVehiculo(List<RefuelingRecordsEntity> tanqueos, String tipo) {
        boolean esMoto = MOTOCICLETA.equals(tipo);
        return tanqueos.stream()
                .filter(t -> vehicleRepository.findById(t.getVehicleId())
                        .map(VehicleEntity::getTipoVehiculo)
                        .map(tv -> esMoto == "MOTOCICLETA".equalsIgnoreCase(tv.getNombreTipo()))
                        .orElse(false))
                .toList();
    }

    private Optional<FuelPerformanceResponse> calcularFila(RefuelingRecordsEntity tanqueo) {
        boolean esMaquina = tanqueo.getMachineId() != null;

        Optional<RefuelingRecordsEntity> anterior = esMaquina
                ? refuelingRecordsRepository.findAnteriorPorMachineId(tanqueo.getMachineId(), tanqueo.getFechaRegistro())
                : refuelingRecordsRepository.findAnteriorPorVehicleId(tanqueo.getVehicleId(), tanqueo.getFechaRegistro());
        if (anterior.isEmpty()) {
            return Optional.empty(); // sin línea base, no se puede proyectar
        }

        Optional<AssetFuelConfigEntity> config = esMaquina
                ? assetFuelConfigRepository.findByMachineId(tanqueo.getMachineId())
                : assetFuelConfigRepository.findByVehicleId(tanqueo.getVehicleId());
        if (config.isEmpty()) {
            return Optional.empty(); // sin consumo estándar configurado, no se puede proyectar
        }

        BigDecimal horometroAnterior = anterior.get().getHorometroKm();
        BigDecimal horometroActual = tanqueo.getHorometroKm();
        BigDecimal ejecutado = horometroActual.subtract(horometroAnterior);
        BigDecimal consumoEstandar = config.get().getConsumoEstandar();

        BigDecimal galonesProyectados = config.get().getUnidadConsumo().endsWith(SUFIJO_POR_HORA)
                ? ejecutado.multiply(consumoEstandar)
                : ejecutado.divide(consumoEstandar, 3, java.math.RoundingMode.HALF_UP);

        BigDecimal diferencia = tanqueo.getCantidadGalones().subtract(galonesProyectados);
        boolean alerta = diferencia.abs().compareTo(galonesProyectados.multiply(tolerancia).abs()) > 0;

        return Optional.of(new FuelPerformanceResponse(
                tanqueo.getId(), tanqueo.getVehicleId(), tanqueo.getMachineId(), tanqueo.getFechaRegistro().toLocalDateTime(),
                horometroAnterior, horometroActual, ejecutado, consumoEstandar,
                galonesProyectados, tanqueo.getCantidadGalones(), diferencia, alerta));
    }
}

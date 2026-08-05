package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.fuel.application.dto.FuelPerformanceResponse;
import com.app.usochicamochabackend.fuel.application.port.GetFuelPerformanceUseCase;
import com.app.usochicamochabackend.fuel.infrastructure.entity.AssetFuelConfigEntity;
import com.app.usochicamochabackend.fuel.infrastructure.entity.RefuelingRecordsEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.AssetFuelConfigRepository;
import com.app.usochicamochabackend.fuel.infrastructure.repository.RefuelingRecordsRepository;
import com.app.usochicamochabackend.machine.infrastructure.entity.MachineEntity;
import com.app.usochicamochabackend.machine.infrastructure.repository.MachineRepository;
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

    private final RefuelingRecordsRepository refuelingRecordsRepository;
    private final AssetFuelConfigRepository assetFuelConfigRepository;
    private final VehicleRepository vehicleRepository;
    private final MachineRepository machineRepository;

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

        // Config vieja/inválida (consumoEstandar<=0 nunca debería poder guardarse
        // desde AssetFuelConfigService, pero se revisa igual aquí como defensa):
        // dividir por cero rompería TODO el reporte, no solo esta fila.
        if (consumoEstandar.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }

        BigDecimal galonesProyectados = FuelConsumptionProjectionUtil.proyectar(
                ejecutado, consumoEstandar, config.get().getUnidadConsumo());

        BigDecimal diferencia = tanqueo.getCantidadGalones().subtract(galonesProyectados);
        // Un horómetro/km que retrocede (dato mal digitado o corregido hacia atrás)
        // siempre se marca como alerta de forma explícita — no depender de que el
        // cálculo normal (proyectado negativo) termine disparándola por coincidencia.
        boolean horometroRetrocedio = ejecutado.signum() < 0;
        boolean alerta = horometroRetrocedio
                || diferencia.abs().compareTo(galonesProyectados.multiply(tolerancia).abs()) > 0;

        String identificacionActivo = esMaquina
                ? machineRepository.findById(tanqueo.getMachineId()).map(MachineEntity::getName).orElse(null)
                : vehicleRepository.findById(tanqueo.getVehicleId()).map(this::placaConMarca).orElse(null);

        return Optional.of(new FuelPerformanceResponse(
                tanqueo.getId(), tanqueo.getVehicleId(), tanqueo.getMachineId(), tanqueo.getFuelTypeId(),
                tanqueo.getFechaRegistro().toLocalDateTime(),
                horometroAnterior, horometroActual, ejecutado, consumoEstandar,
                galonesProyectados, tanqueo.getCantidadGalones(), diferencia, alerta,
                identificacionActivo, tanqueo.getEsFull()));
    }

    private String placaConMarca(VehicleEntity vehicle) {
        String marca = vehicle.getMarca() != null ? vehicle.getMarca().getDescripcion() : null;
        return marca != null ? vehicle.getPlaca() + " — " + marca : vehicle.getPlaca();
    }
}

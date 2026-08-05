package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.fuel.infrastructure.entity.AssetFuelConfigEntity;
import com.app.usochicamochabackend.fuel.infrastructure.entity.RefuelingRecordsEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.AssetFuelConfigRepository;
import com.app.usochicamochabackend.fuel.infrastructure.repository.RefuelingRecordsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Optional;

/**
 * Discrepancia por "Full" declarado pero cantidad tanqueada insuficiente para
 * llenar el tanque: un tanqueo marcado {@code esFull=true} debería aportar
 * aproximadamente lo que el consumo estándar del activo proyecta desde el
 * tanqueo anterior (mismo cálculo que {@link FuelPerformanceService} usa para
 * Rendimiento) — si tanqueó bastante menos de eso, o marcó "Full" sin serlo, o
 * el consumo real fue mucho menor al estándar (línea base desactualizada).
 * Compartido por cualquier consumidor de tanqueos, mismo patrón que
 * {@link AssetFuelCapacityService}/{@link FuelPriceAnomalyService}.
 */
@Service
@RequiredArgsConstructor
public class FuelFullConsistencyService {

    private final RefuelingRecordsRepository refuelingRecordsRepository;
    private final AssetFuelConfigRepository assetFuelConfigRepository;

    // Misma tolerancia que Rendimiento — un "Full" insuficiente es, en el fondo, el
    // mismo tipo de desviación de consumo que ya se tolera ahí.
    @Value("${app.fuel.rendimiento-desviacion-tolerancia-porcentaje:0.15}")
    private BigDecimal tolerancia;

    public boolean fullInconsistente(Integer vehicleId, Long machineId, Boolean esFull, BigDecimal cantidadGalones,
            BigDecimal horometroKm, Timestamp fechaRegistro, BigDecimal capacidadConfiguradaGal) {
        BigDecimal esperado = cantidadEsperadaParaLleno(
                vehicleId, machineId, esFull, horometroKm, fechaRegistro, capacidadConfiguradaGal);
        if (esperado == null || cantidadGalones == null) {
            return false;
        }
        return cantidadGalones.compareTo(esperado.multiply(BigDecimal.ONE.subtract(tolerancia))) < 0;
    }

    /**
     * Cuánto se esperaría que hiciera falta para volver a llenar el tanque, en base
     * al consumo estándar configurado y el recorrido/horas desde el tanqueo anterior
     * — acotado a la capacidad configurada del tanque (nunca puede hacer falta más
     * de lo que el tanque completo admite). Null cuando no hay línea base para
     * proyectar (sin tanqueo anterior, sin consumo estándar configurado, o el
     * tanqueo no está marcado como "Full").
     */
    public BigDecimal cantidadEsperadaParaLleno(Integer vehicleId, Long machineId, Boolean esFull,
            BigDecimal horometroKm, Timestamp fechaRegistro, BigDecimal capacidadConfiguradaGal) {
        if (!Boolean.TRUE.equals(esFull) || horometroKm == null) {
            return null;
        }
        Optional<RefuelingRecordsEntity> anterior = machineId != null
                ? refuelingRecordsRepository.findAnteriorPorMachineId(machineId, fechaRegistro)
                : refuelingRecordsRepository.findAnteriorPorVehicleId(vehicleId, fechaRegistro);
        if (anterior.isEmpty()) {
            return null;
        }
        Optional<AssetFuelConfigEntity> config = machineId != null
                ? assetFuelConfigRepository.findByMachineId(machineId)
                : assetFuelConfigRepository.findByVehicleId(vehicleId);
        if (config.isEmpty()) {
            return null;
        }
        BigDecimal consumoEstandar = config.get().getConsumoEstandar();
        if (consumoEstandar == null || consumoEstandar.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal ejecutado = horometroKm.subtract(anterior.get().getHorometroKm());
        if (ejecutado.signum() < 0) {
            // Horómetro/km que retrocede: dato mal digitado, ya se marca aparte en
            // Rendimiento — proyectar un recorrido negativo no tiene sentido acá.
            return null;
        }
        BigDecimal proyectado = FuelConsumptionProjectionUtil.proyectar(ejecutado, consumoEstandar, config.get().getUnidadConsumo());
        return capacidadConfiguradaGal != null ? proyectado.min(capacidadConfiguradaGal) : proyectado;
    }
}

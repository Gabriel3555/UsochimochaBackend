package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.fuel.infrastructure.entity.AssetFuelConfigEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.AssetFuelConfigRepository;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.VehicleEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Discrepancia por capacidad de tanque excedida: un tanqueo nunca puede cargar más
 * galones/m³ de los que el tanque del activo físicamente admite. Compartido por
 * cualquier consumidor de tanqueos (creación, edición, listados, reportes) para no
 * duplicar el cruce con `asset_fuel_config` en cada uno — funciona igual para
 * vehículos, motocicletas (viven en `vehiculos`) y maquinaria, ya que
 * `asset_fuel_config` no distingue tipo de activo, solo vehicleId/machineId.
 */
@Service
@RequiredArgsConstructor
public class AssetFuelCapacityService {

    private static final String MOTOCICLETA = "MOTOCICLETA";

    private final AssetFuelConfigRepository assetFuelConfigRepository;
    private final VehicleRepository vehicleRepository;

    @Value("${app.fuel.cantidad-maxima-razonable-moto-gal:15}")
    private BigDecimal maximoRazonableMoto;

    @Value("${app.fuel.cantidad-maxima-razonable-vehiculo-gal:60}")
    private BigDecimal maximoRazonableVehiculo;

    @Value("${app.fuel.cantidad-maxima-razonable-maquinaria-gal:500}")
    private BigDecimal maximoRazonableMaquinaria;

    public boolean excedeCapacidad(Integer vehicleId, Long machineId, BigDecimal cantidadGalones) {
        if (cantidadGalones == null) {
            return false;
        }
        BigDecimal capacidad = capacidadConfigurada(vehicleId, machineId);
        if (capacidad == null) {
            return false;
        }
        return cantidadGalones.compareTo(capacidad) > 0;
    }

    /**
     * Capacidad de tanque configurada en {@code asset_fuel_config.tanqueCapacidadGal}
     * para este activo en particular — null si no tiene configuración. Se expone por
     * separado de {@link #excedeCapacidad} para que los consumidores puedan mostrar
     * el valor de referencia junto a la alerta ("excedió los 50 gal configurados"),
     * no solo el booleano.
     */
    public BigDecimal capacidadConfigurada(Integer vehicleId, Long machineId) {
        var config = machineId != null
                ? assetFuelConfigRepository.findByMachineId(machineId)
                : assetFuelConfigRepository.findByVehicleId(vehicleId);
        return config.map(AssetFuelConfigEntity::getTanqueCapacidadGal).orElse(null);
    }

    /**
     * Discrepancia por cantidad fuera de un rango razonable para el TIPO de activo
     * (no la capacidad configurada de este activo en particular, que ya cubre
     * {@link #excedeCapacidad} — este chequeo aplica siempre, incluso sin
     * `asset_fuel_config`, para atrapar errores de digitación obvios como un
     * tanqueo de 300 gal en una moto).
     */
    public boolean cantidadFueraDeRangoTipico(Integer vehicleId, Long machineId, BigDecimal cantidadGalones) {
        if (cantidadGalones == null) {
            return false;
        }
        return cantidadGalones.compareTo(maximoTipico(vehicleId, machineId)) > 0;
    }

    /**
     * Tope razonable aplicado según el tipo de activo (máquina/moto/vehículo) —
     * expuesto por separado de {@link #cantidadFueraDeRangoTipico} para que los
     * consumidores puedan mostrar contra qué límite se comparó, no solo el booleano.
     * A diferencia de {@link #capacidadConfigurada}, siempre tiene un valor (son
     * constantes por tipo, no dependen de {@code asset_fuel_config}).
     */
    public BigDecimal maximoTipico(Integer vehicleId, Long machineId) {
        if (machineId != null) {
            return maximoRazonableMaquinaria;
        }
        boolean esMoto = vehicleRepository.findById(vehicleId)
                .map(VehicleEntity::getTipoVehiculo)
                .map(tv -> MOTOCICLETA.equalsIgnoreCase(tv.getNombreTipo()))
                .orElse(false);
        return esMoto ? maximoRazonableMoto : maximoRazonableVehiculo;
    }
}

package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.fuel.infrastructure.entity.AssetFuelConfigEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.AssetFuelConfigRepository;
import lombok.RequiredArgsConstructor;
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

    private final AssetFuelConfigRepository assetFuelConfigRepository;

    public boolean excedeCapacidad(Integer vehicleId, Long machineId, BigDecimal cantidadGalones) {
        if (cantidadGalones == null) {
            return false;
        }
        var config = machineId != null
                ? assetFuelConfigRepository.findByMachineId(machineId)
                : assetFuelConfigRepository.findByVehicleId(vehicleId);
        return config.map(AssetFuelConfigEntity::getTanqueCapacidadGal)
                .filter(capacidad -> capacidad != null)
                .map(capacidad -> cantidadGalones.compareTo(capacidad) > 0)
                .orElse(false);
    }
}

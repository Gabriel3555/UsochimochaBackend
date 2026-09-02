package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.exception.ResourceNotFoundException;
import com.app.usochicamochabackend.fuel.application.port.AdjustFuelInventoryUseCase;
import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelInventoryEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class FuelInventoryService implements AdjustFuelInventoryUseCase {

    private final FuelInventoryRepository fuelInventoryRepository;

    @Override
    public void increment(String areaCosto, Long fuelTypeId, BigDecimal cantidad) {
        FuelInventoryEntity inventory = findInventory(areaCosto, fuelTypeId);
        inventory.setCantidadDisponible(inventory.getCantidadDisponible().add(cantidad));
        fuelInventoryRepository.save(inventory);
    }

    @Override
    public void decrement(String areaCosto, Long fuelTypeId, BigDecimal cantidad) {
        FuelInventoryEntity inventory = findInventory(areaCosto, fuelTypeId);
        BigDecimal saldoResultante = inventory.getCantidadDisponible().subtract(cantidad);
        if (saldoResultante.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Stock insuficiente en almacén (" + areaCosto + ") para el combustible solicitado.");
        }
        inventory.setCantidadDisponible(saldoResultante);
        fuelInventoryRepository.save(inventory);
    }

    private FuelInventoryEntity findInventory(String areaCosto, Long fuelTypeId) {
        return fuelInventoryRepository.findByAreaCostoAndFuelTypeId(areaCosto, fuelTypeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe inventario para area_costo=" + areaCosto + " y fuel_type_id=" + fuelTypeId));
    }
}

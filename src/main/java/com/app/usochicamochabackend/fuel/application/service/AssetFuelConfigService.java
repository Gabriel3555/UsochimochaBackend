package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.actions.application.port.SaveActionUseCase;
import com.app.usochicamochabackend.exception.ResourceNotFoundException;
import com.app.usochicamochabackend.fuel.application.dto.AssetFuelConfigRequest;
import com.app.usochicamochabackend.fuel.application.dto.AssetFuelConfigResponse;
import com.app.usochicamochabackend.fuel.application.port.ManageAssetFuelConfigUseCase;
import com.app.usochicamochabackend.fuel.infrastructure.entity.AssetFuelConfigEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.AssetFuelConfigRepository;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelTypesRepository;
import com.app.usochicamochabackend.machine.infrastructure.repository.MachineRepository;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AssetFuelConfigService implements ManageAssetFuelConfigUseCase {

    private static final String GAL_POR_HORA = "GAL_POR_HORA";
    private static final String KM_POR_GALON = "KM_POR_GALON";

    private final AssetFuelConfigRepository assetFuelConfigRepository;
    private final FuelTypesRepository fuelTypesRepository;
    private final VehicleRepository vehicleRepository;
    private final MachineRepository machineRepository;
    private final SaveActionUseCase saveActionUseCase;

    @Override
    public List<AssetFuelConfigResponse> listar() {
        return assetFuelConfigRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional
    public AssetFuelConfigResponse configurarVehiculo(Integer vehicleId, AssetFuelConfigRequest request) {
        validarRequest(request, KM_POR_GALON);
        if (!vehicleRepository.existsById(vehicleId)) {
            throw new ResourceNotFoundException("No existe el vehículo con id=" + vehicleId);
        }
        AssetFuelConfigEntity entity = assetFuelConfigRepository.findByVehicleId(vehicleId)
                .orElseGet(() -> AssetFuelConfigEntity.builder().vehicleId(vehicleId).build());
        aplicarRequest(entity, request);
        entity = assetFuelConfigRepository.save(entity);

        saveActionUseCase.save("Se configuró el consumo estándar del vehículo id=" + vehicleId);
        return mapToResponse(entity);
    }

    @Override
    @Transactional
    public AssetFuelConfigResponse configurarMaquina(Long machineId, AssetFuelConfigRequest request) {
        validarRequest(request, GAL_POR_HORA);
        if (!machineRepository.existsById(machineId)) {
            throw new ResourceNotFoundException("No existe la máquina con id=" + machineId);
        }
        AssetFuelConfigEntity entity = assetFuelConfigRepository.findByMachineId(machineId)
                .orElseGet(() -> AssetFuelConfigEntity.builder().machineId(machineId).build());
        aplicarRequest(entity, request);
        entity = assetFuelConfigRepository.save(entity);

        saveActionUseCase.save("Se configuró el consumo estándar de la máquina id=" + machineId);
        return mapToResponse(entity);
    }

    private void validarRequest(AssetFuelConfigRequest request, String unidadEsperada) {
        if (request.fuelTypeDefaultId() == null || request.consumoEstandar() == null || request.unidadConsumo() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "fuelTypeDefaultId, consumoEstandar y unidadConsumo son obligatorios.");
        }
        if (!request.unidadConsumo().equals(unidadEsperada)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "unidadConsumo debe ser " + unidadEsperada + " para este tipo de activo.");
        }
        if (!fuelTypesRepository.existsById(request.fuelTypeDefaultId())) {
            throw new ResourceNotFoundException("No existe el tipo de combustible con id=" + request.fuelTypeDefaultId());
        }
    }

    private void aplicarRequest(AssetFuelConfigEntity entity, AssetFuelConfigRequest request) {
        entity.setFuelTypeDefaultId(request.fuelTypeDefaultId());
        entity.setConsumoEstandar(request.consumoEstandar());
        entity.setUnidadConsumo(request.unidadConsumo());
        entity.setTanqueCapacidadGal(request.tanqueCapacidadGal());
    }

    private AssetFuelConfigResponse mapToResponse(AssetFuelConfigEntity entity) {
        return new AssetFuelConfigResponse(
                entity.getId(),
                entity.getVehicleId(),
                entity.getMachineId(),
                entity.getFuelTypeDefaultId(),
                entity.getConsumoEstandar(),
                entity.getUnidadConsumo(),
                entity.getTanqueCapacidadGal());
    }
}

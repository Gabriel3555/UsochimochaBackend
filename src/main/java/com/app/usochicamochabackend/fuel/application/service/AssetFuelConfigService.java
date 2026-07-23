package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.actions.application.port.SaveActionUseCase;
import com.app.usochicamochabackend.exception.ResourceNotFoundException;
import com.app.usochicamochabackend.fuel.application.dto.AssetFuelConfigRequest;
import com.app.usochicamochabackend.fuel.application.dto.AssetFuelConfigResponse;
import com.app.usochicamochabackend.fuel.application.port.ManageAssetFuelConfigUseCase;
import com.app.usochicamochabackend.fuel.infrastructure.entity.AssetFuelConfigEntity;
import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelTypesEntity;
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
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AssetFuelConfigService implements ManageAssetFuelConfigUseCase {

    // La unidad de consumo esperada depende de dos cosas: la forma que dicta el tipo
    // de activo (KM_POR_X para vehículo, X_POR_HORA para máquina) y la unidad física del
    // combustible seleccionado (GALON o M3, ver fuel_types.unidad_medida) — no es fija.
    private static final Map<String, String> UNIDAD_VEHICULO = Map.of("GALON", "KM_POR_GALON", "M3", "KM_POR_M3");
    private static final Map<String, String> UNIDAD_MAQUINA = Map.of("GALON", "GAL_POR_HORA", "M3", "M3_POR_HORA");

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
        validarRequest(request, UNIDAD_VEHICULO);
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
        validarRequest(request, UNIDAD_MAQUINA);
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

    private void validarRequest(AssetFuelConfigRequest request, Map<String, String> unidadesPorMedida) {
        if (request.fuelTypeDefaultId() == null || request.consumoEstandar() == null || request.unidadConsumo() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "fuelTypeDefaultId, consumoEstandar y unidadConsumo son obligatorios.");
        }
        FuelTypesEntity fuelType = fuelTypesRepository.findById(request.fuelTypeDefaultId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe el tipo de combustible con id=" + request.fuelTypeDefaultId()));

        String unidadEsperada = unidadesPorMedida.get(fuelType.getUnidadMedida());
        if (!request.unidadConsumo().equals(unidadEsperada)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "unidadConsumo debe ser " + unidadEsperada + " para este tipo de activo con combustible en " + fuelType.getUnidadMedida() + ".");
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

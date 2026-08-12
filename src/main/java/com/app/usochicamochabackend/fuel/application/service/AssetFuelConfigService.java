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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AssetFuelConfigService implements ManageAssetFuelConfigUseCase {

    // La unidad de consumo solo se restringe por la familia física del combustible
    // seleccionado (GALON o M3, ver fuel_types.unidad_medida) — NO por el tipo de
    // activo. FuelPerformanceService decide la fórmula únicamente por el sufijo de
    // unidadConsumo ("_POR_HORA" o no), sin mirar si es vehículo o máquina, así que
    // un vehículo trackeado por horómetro o una máquina trackeada por km/gal son
    // casos válidos igual — el sistema debe estar preparado para cualquier caso.
    private static final Map<String, List<String>> UNIDADES_VALIDAS_POR_COMBUSTIBLE =
            Map.of("GALON", List.of("KM_POR_GALON", "HORA_POR_GALON"), "M3", List.of("KM_POR_M3", "HORA_POR_M3"));

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
        validarRequest(request);
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
        validarRequest(request);
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

    private void validarRequest(AssetFuelConfigRequest request) {
        if (request.fuelTypeDefaultId() == null || request.consumoEstandar() == null || request.unidadConsumo() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "fuelTypeDefaultId, consumoEstandar y unidadConsumo son obligatorios.");
        }
        // consumoEstandar<=0 provocaría una división por cero al proyectar el
        // Rendimiento (FuelPerformanceService) — se rechaza aquí, en el alta.
        if (request.consumoEstandar().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "consumoEstandar debe ser mayor a 0.");
        }
        FuelTypesEntity fuelType = fuelTypesRepository.findById(request.fuelTypeDefaultId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe el tipo de combustible con id=" + request.fuelTypeDefaultId()));

        List<String> unidadesValidas = UNIDADES_VALIDAS_POR_COMBUSTIBLE.get(fuelType.getUnidadMedida());
        if (unidadesValidas == null || !unidadesValidas.contains(request.unidadConsumo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "unidadConsumo debe ser una de " + unidadesValidas + " para combustible en " + fuelType.getUnidadMedida() + ".");
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

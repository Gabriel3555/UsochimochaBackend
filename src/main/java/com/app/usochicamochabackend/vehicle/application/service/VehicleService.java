package com.app.usochicamochabackend.vehicle.application.service;

import com.app.usochicamochabackend.catalog.infrastructure.repository.UbicacionRepository;
import com.app.usochicamochabackend.common.text.InputTextNormalizer;
import com.app.usochicamochabackend.mapper.VehicleMapper;
import com.app.usochicamochabackend.vehicle.application.dto.VehicleRequest;
import com.app.usochicamochabackend.vehicle.application.dto.VehicleResponse;
import com.app.usochicamochabackend.vehicle.application.port.VehicleUseCase;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.VehicleEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleService implements VehicleUseCase {

    private final VehicleRepository vehicleRepository;
    private final UbicacionRepository ubicacionRepository;

    @Override
    public List<VehicleResponse> findAllVehicles() {
        return vehicleRepository.findAllActiveVehicles().stream()
                .map(VehicleMapper::toResponse)
                .toList();
    }

    @Override
    public VehicleResponse findByPlaca(String placa) {
        String p = InputTextNormalizer.normalizePlaca(placa);
        return vehicleRepository.findVehicleDetailByPlaca(p)
                .map(VehicleMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehículo no encontrado con placa: " + p));
    }

    @Override
    public VehicleResponse createVehicle(VehicleRequest request) {
        VehicleRequest req = request.normalized();
        if (vehicleRepository.findByPlaca(req.placa()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe un vehículo con esta placa");
        }
        if (req.idUbicacionBase() != null && !ubicacionRepository.existsById(req.idUbicacionBase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ubicación no válida");
        }

        var ubi = req.idUbicacionBase() != null
                ? ubicacionRepository.getReferenceById(req.idUbicacionBase())
                : null;
        VehicleEntity entity = VehicleEntity.builder()
                .placa(req.placa())
                .idMarca(req.idMarca())
                .idTipoVehiculo(req.idTipoVehiculo())
                .kilometrajeActual(req.kilometrajeActual())
                .belongsTo(req.belongsTo())
                .ubicacionBase(ubi)
                .activo(true)
                .fuelTankCapacityGallons(req.fuelTankCapacityGallons())
                .factoryEfficiencyKmPerGallon(req.factoryEfficiencyKmPerGallon())
                .factoryEfficiencyUnit(req.factoryEfficiencyUnit())
                .build();

        vehicleRepository.save(entity);
        return findByPlaca(entity.getPlaca());
    }

    @Override
    public VehicleResponse updateVehicle(Integer id, VehicleRequest request) {
        VehicleRequest req = request.normalized();
        VehicleEntity entity = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehículo no encontrado"));

        if (!entity.getPlaca().equals(req.placa()) && vehicleRepository.findByPlaca(req.placa()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe otro vehículo con esta placa");
        }

        if (req.idUbicacionBase() != null && !ubicacionRepository.existsById(req.idUbicacionBase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ubicación no válida");
        }

        entity.setPlaca(req.placa());
        entity.setIdMarca(req.idMarca());
        entity.setIdTipoVehiculo(req.idTipoVehiculo());
        entity.setKilometrajeActual(req.kilometrajeActual());
        entity.setBelongsTo(req.belongsTo());
        entity.setUbicacionBase(req.idUbicacionBase() != null
                ? ubicacionRepository.getReferenceById(req.idUbicacionBase())
                : null);
        entity.setActivo(req.activo() != null ? req.activo() : entity.getActivo());
        if (req.fuelTankCapacityGallons() != null) {
            entity.setFuelTankCapacityGallons(req.fuelTankCapacityGallons());
        }
        if (req.factoryEfficiencyKmPerGallon() != null) {
            entity.setFactoryEfficiencyKmPerGallon(req.factoryEfficiencyKmPerGallon());
        }
        if (req.factoryEfficiencyUnit() != null) {
            entity.setFactoryEfficiencyUnit(req.factoryEfficiencyUnit());
        }

        vehicleRepository.save(entity);
        return findByPlaca(entity.getPlaca());
    }

    @Override
    public void deleteVehicle(Integer id) {
        VehicleEntity entity = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehículo no encontrado"));
        entity.setActivo(false);
        vehicleRepository.save(entity);
    }
}

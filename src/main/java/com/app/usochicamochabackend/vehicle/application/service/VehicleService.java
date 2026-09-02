package com.app.usochicamochabackend.vehicle.application.service;

import com.app.usochicamochabackend.actions.application.port.SaveActionUseCase;
import com.app.usochicamochabackend.auth.application.dto.UserPrincipal;
import com.app.usochicamochabackend.catalog.infrastructure.repository.UbicacionRepository;
import com.app.usochicamochabackend.common.text.InputTextNormalizer;
import com.app.usochicamochabackend.exception.VehicleSoftDeletedConflictException;
import com.app.usochicamochabackend.mapper.VehicleMapper;
import com.app.usochicamochabackend.notifications.application.PreventiveAlertCalculationService;
import com.app.usochicamochabackend.vehicle.application.dto.VehicleRequest;
import com.app.usochicamochabackend.vehicle.application.dto.VehicleResponse;
import com.app.usochicamochabackend.vehicle.application.port.VehicleUseCase;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.VehicleEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VehicleService implements VehicleUseCase {

    private final VehicleRepository vehicleRepository;
    private final UbicacionRepository ubicacionRepository;
    private final SaveActionUseCase saveActionUseCase;
    private final PreventiveAlertCalculationService preventiveAlertCalculationService;

    @Override
    public List<VehicleResponse> findAllVehicles() {
        return vehicleRepository.findAllActiveVehiclesWithDocuments().stream()
                .map(VehicleMapper::toResponse)
                .toList();
    }

    @Override
    public VehicleResponse findByPlaca(String placa) {
        String p = InputTextNormalizer.normalizePlaca(placa);
        return vehicleRepository.findActiveByPlaca(p)
                .map(VehicleMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehículo no encontrado con placa: " + p));
    }

    @Override
    @Transactional
    public VehicleResponse createVehicle(VehicleRequest request) {
        VehicleRequest req = request.normalized();

        Optional<VehicleEntity> existingVehicle = vehicleRepository.findByPlaca(req.placa());

        if (existingVehicle.isPresent()) {
            VehicleEntity existing = existingVehicle.get();
            if (existing.getActivo()) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ya existe un vehículo con placa: " + req.placa()
                );
            } else {
                throw new VehicleSoftDeletedConflictException(
                    "El vehículo con placa " + req.placa() +
                    " fue eliminado. Opciones: restaurar o crear con otra placa",
                    VehicleMapper.toResponse(existing)
                );
            }
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
                .build();

        vehicleRepository.save(entity);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            saveActionUseCase.save("El vehículo " + entity.getPlaca() + " ha sido creado por " + userPrincipal.username());
        } else {
            saveActionUseCase.save("El vehículo " + entity.getPlaca() + " ha sido creado");
        }

        return findByPlaca(entity.getPlaca());
    }

    @Override
    @Transactional
    public VehicleResponse updateVehicle(Integer id, VehicleRequest request) {
        VehicleRequest req = request.normalized();
        VehicleEntity entity = vehicleRepository.findActiveById(id)
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

        vehicleRepository.save(entity);

        // Recalcular alertas de inmediato: el kilometraje pudo haber cambiado al editar.
        preventiveAlertCalculationService.calculateAndEmitAlerts();

        return findByPlaca(entity.getPlaca());
    }

    @Override
    @Transactional
    public void deleteVehicle(Integer id) {
        VehicleEntity entity = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehículo no encontrado"));
        entity.setActivo(false);
        vehicleRepository.save(entity);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            saveActionUseCase.save("El vehículo " + entity.getPlaca() + " ha sido eliminado por " + userPrincipal.username());
        } else {
            saveActionUseCase.save("El vehículo " + entity.getPlaca() + " ha sido eliminado");
        }
    }

    @Transactional
    public VehicleResponse restoreVehicle(Integer id) {
        VehicleEntity vehicle = vehicleRepository.findDeletedById(id)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Vehículo eliminado no encontrado con id: " + id
                ));

        vehicle.setActivo(true);
        vehicleRepository.save(vehicle);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            saveActionUseCase.save("El vehículo " + vehicle.getPlaca() + " ha sido restaurado por " + userPrincipal.username());
        } else {
            saveActionUseCase.save("El vehículo " + vehicle.getPlaca() + " ha sido restaurado");
        }

        return findByPlaca(vehicle.getPlaca());
    }
}

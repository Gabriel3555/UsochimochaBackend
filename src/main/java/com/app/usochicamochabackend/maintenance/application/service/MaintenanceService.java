package com.app.usochicamochabackend.maintenance.application.service;

import com.app.usochicamochabackend.actions.application.port.SaveActionUseCase;
import com.app.usochicamochabackend.auth.application.dto.UserPrincipal;
import com.app.usochicamochabackend.maintenance.application.dto.MaintenanceRequest;
import com.app.usochicamochabackend.maintenance.application.dto.MaintenanceResponse;
import com.app.usochicamochabackend.maintenance.application.port.MaintenanceUseCase;
import com.app.usochicamochabackend.maintenance.infrastructure.entity.MaintenanceEntity;
import com.app.usochicamochabackend.maintenance.infrastructure.repository.MaintenanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MaintenanceService implements MaintenanceUseCase {

    private final MaintenanceRepository maintenanceRepository;
    private final SaveActionUseCase saveActionUseCase;

    @Override
    @Transactional
    public void registerMaintenance(MaintenanceRequest request) {
        MaintenanceEntity entity = MaintenanceEntity.builder()
                .fecha(request.fecha() != null ? request.fecha() : java.time.LocalDateTime.now())
                .idVehiculo(request.idVehiculo())
                .idUbicacion(request.idUbicacion())
                .responsableAsignado(request.responsableAsignado())
                .kilometraje(request.kilometraje())
                .tipoMantenimiento(request.tipoMantenimiento())
                .repuestosMantenimiento(request.repuestosMantenimiento())
                .tallerResponsable(request.tallerResponsable())
                .observaciones(request.observaciones())
                .build();
        maintenanceRepository.save(entity);

        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        saveActionUseCase.save("El usuario " + userPrincipal.username() + 
            " ha registrado un mantenimiento " + entity.getTipoMantenimiento() + 
            " para el vehículo con ID: " + entity.getIdVehiculo());
    }

    @Override
    public List<MaintenanceResponse> getMotosMaintenance() {
        return maintenanceRepository.findAllByVehicleTypeName("MOTOCICLETA").stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<MaintenanceResponse> getVehiclesMaintenance() {
        // En un futuro podríamos filtrar por lo que NO es moto, o por tipos específicos
        return maintenanceRepository.findAll().stream()
                .filter(m -> !m.getVehiculo().getTipoVehiculo().getNombreTipo().equalsIgnoreCase("MOTOCICLETA"))
                .map(this::mapToResponse)
                .toList();
    }

    private MaintenanceResponse mapToResponse(MaintenanceEntity entity) {
        return new MaintenanceResponse(
            entity.getId(),
            entity.getFecha(),
            entity.getVehiculo() != null ? entity.getVehiculo().getPlaca() : "N/A",
            entity.getUbicacion() != null ? entity.getUbicacion().getNombreUbicacion() : "N/A",
            entity.getResponsableAsignado(),
            entity.getKilometraje(),
            entity.getTipoMantenimiento(),
            entity.getRepuestosMantenimiento(),
            entity.getTallerResponsable(),
            entity.getObservaciones()
        );
    }
}

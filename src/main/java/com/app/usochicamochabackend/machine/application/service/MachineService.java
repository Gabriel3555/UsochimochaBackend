package com.app.usochicamochabackend.machine.application.service;

import com.app.usochicamochabackend.actions.application.port.SaveActionUseCase;
import com.app.usochicamochabackend.auth.application.dto.UserPrincipal;
import com.app.usochicamochabackend.common.text.InputTextNormalizer;
import com.app.usochicamochabackend.exception.ResourceNotFoundException;
import com.app.usochicamochabackend.machine.application.dto.MachineRequest;
import com.app.usochicamochabackend.machine.application.dto.MachineResponse;
import com.app.usochicamochabackend.machine.application.port.*;
import com.app.usochicamochabackend.machine.infrastructure.entity.MachineEntity;
import com.app.usochicamochabackend.machine.infrastructure.repository.MachineRepository;
import com.app.usochicamochabackend.mapper.MachineMapper;
import com.app.usochicamochabackend.notifications.application.NotificationService;
import com.app.usochicamochabackend.notifications.application.PreventiveAlertCalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MachineService implements FindMachineByIdUseCase, FindAllMachinesUseCase, DeleteMachineUseCase, CreateMachineUseCase, UpdateMachineUseCase {

    private final MachineRepository machineRepository;
    private final SaveActionUseCase saveActionUseCase;
    private final NotificationService notificationService;
    private final PreventiveAlertCalculationService preventiveAlertCalculationService;

    @Override
    public MachineResponse createMachine(MachineRequest machineRequest) {
        MachineRequest req = machineRequest.normalized();
        MachineEntity savedMachine = machineRepository.save(MachineEntity.builder()
                .name(req.name()).model(req.model()).belongsTo(req.belongsTo())
                .soat(req.soat()).brand(req.brand()).runt(req.runt()).status(true)
                .numEngine(req.numEngine()).numInterIdentification(req.numInterIdentification())
                .build());

        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String username = "anonymous";

            if (principal instanceof UserPrincipal userPrincipal) {
                username = userPrincipal.username();
            }

            saveActionUseCase.save("El usuario " + username +
                    " ha creado la máquina " + savedMachine.getName());
        } catch (Exception e) {
            // If no authentication or error getting principal, use anonymous
            saveActionUseCase.save("Usuario anonymous ha creado la máquina " + savedMachine.getName());
        }

        // Recalcular alertas de inmediato: la máquina puede llegar con SOAT/seguro
        // todo riesgo ya vencidos o próximos a vencer.
        preventiveAlertCalculationService.calculateAndEmitAlerts();

        return MachineMapper.toResponse(savedMachine);
    }

    @Override
    public List<MachineResponse> findAllMachines() {


        return machineRepository.findAll().stream()
                .filter(MachineEntity::getStatus)
                .map(MachineMapper::toResponse)
                .toList();
    }

    @Override
    public MachineResponse findMachineById(Long id) {
        MachineEntity machine = machineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Machine not found with ID: " + id));

        if (!machine.getStatus()) {
            throw new ResourceNotFoundException("Machine not found with ID: " + id);
        }

        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String username = "anonymous";

            if (principal instanceof UserPrincipal userPrincipal) {
                username = userPrincipal.username();
            }

            saveActionUseCase.save("El usuario " + username + " ha observado la maquina " + machine.getName());
        } catch (Exception e) {
            // If no authentication or error getting principal, use anonymous
            saveActionUseCase.save("Usuario anonymous ha observado la maquina " + machine.getName());
        }


        return MachineMapper.toResponse(machine);
    }

    @Override
    public MachineResponse updateMachine(MachineRequest machineRequest, Long id) {
        MachineRequest req = machineRequest.normalized();
        MachineEntity currentMachine = machineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Machine not found with ID: " + id));

        if (!currentMachine.getStatus()) {
            throw new ResourceNotFoundException("Machine not found with ID: " + id);
        }

        MachineEntity savedMachine = machineRepository.save(MachineEntity.builder()
                .id(id).name(req.name()).model(req.model()).belongsTo(req.belongsTo())
                .soat(req.soat()).brand(req.brand()).runt(req.runt()).status(true)
                .numEngine(req.numEngine()).numInterIdentification(req.numInterIdentification())
                .horometroActual(currentMachine.getHorometroActual())
                .build());

        List<String> cambios = new ArrayList<>();

        if (!Objects.equals(currentMachine.getName(), savedMachine.getName())) {
            cambios.add("name: " + currentMachine.getName() + " -> " + savedMachine.getName());
        }
        if (!Objects.equals(currentMachine.getModel(), savedMachine.getModel())) {
            cambios.add("model: " + currentMachine.getModel() + " -> " + savedMachine.getModel());
        }
        if (!Objects.equals(currentMachine.getBrand(), savedMachine.getBrand())) {
            cambios.add("brand: " + currentMachine.getBrand() + " -> " + savedMachine.getBrand());
        }
        if (!Objects.equals(currentMachine.getNumEngine(), savedMachine.getNumEngine())) {
            cambios.add("numEngine: " + currentMachine.getNumEngine() + " -> " + savedMachine.getNumEngine());
        }
        if (!Objects.equals(currentMachine.getNumInterIdentification(), savedMachine.getNumInterIdentification())) {
            cambios.add("numInterIdentification: " + currentMachine.getNumInterIdentification() + " -> " + savedMachine.getNumInterIdentification());
        }
        if (!Objects.equals(currentMachine.getSoat(), savedMachine.getSoat())) {
            cambios.add("soat: " + currentMachine.getSoat() + " -> " + savedMachine.getSoat());
        }
        if (!Objects.equals(currentMachine.getRunt(), savedMachine.getRunt())) {
            cambios.add("runt: " + currentMachine.getRunt() + " -> " + savedMachine.getRunt());
        }
        if (!Objects.equals(currentMachine.getStatus(), savedMachine.getStatus())) {
            cambios.add("status: " + currentMachine.getStatus() + " -> " + savedMachine.getStatus());
        }

        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String username = "anonymous";

            if (principal instanceof UserPrincipal userPrincipal) {
                username = userPrincipal.username();
            }

            String mensaje = "El usuario " + username +
                    " ha actualizado la máquina " + savedMachine.getName();

        if (!cambios.isEmpty()) {
            mensaje += ". Cambios: " + String.join(", ", cambios);
        }

        saveActionUseCase.save(mensaje);
        } catch (Exception e) {
            // If no authentication or error getting principal, use anonymous
            String mensaje = "Usuario anonymous ha actualizado la máquina " + savedMachine.getName();
            saveActionUseCase.save(mensaje);
        }

        // Recalcular alertas de inmediato si cambió el SOAT o el seguro todo riesgo,
        // para que la alerta previa desaparezca/actualice sin esperar al cron diario.
        if (cambios.stream().anyMatch(c -> c.startsWith("soat:") || c.startsWith("runt:"))) {
            preventiveAlertCalculationService.calculateAndEmitAlerts();
        }

        return MachineMapper.toResponse(savedMachine);
    }

    @Override
    public void deleteMachine(Long id) {
        MachineEntity machine = machineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Machine not found with ID: " + id));

        if (!machine.getStatus()) {
            throw new ResourceNotFoundException("Machine not found with ID: " + id);
        }

        machine.setStatus(false);
        machineRepository.save(machine);

        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        saveActionUseCase.save("El usuario " + userPrincipal.username() +
                " ha eliminado la máquina " + machine.getName());

    }
}

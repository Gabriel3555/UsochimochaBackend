package com.app.usochicamochabackend.update.application.service;

import com.app.usochicamochabackend.actions.application.port.SaveActionUseCase;
import com.app.usochicamochabackend.auth.application.dto.UserPrincipal;
import com.app.usochicamochabackend.exception.BadRequestException;
import com.app.usochicamochabackend.exception.ResourceNotFoundException;
import com.app.usochicamochabackend.machine.infrastructure.entity.MachineEntity;
import com.app.usochicamochabackend.machine.infrastructure.repository.MachineRepository;
import com.app.usochicamochabackend.mapper.MachineMapper;
import com.app.usochicamochabackend.mapper.OilChangeMapper;
import com.app.usochicamochabackend.notifications.application.NotificationService;
import com.app.usochicamochabackend.review.infrastructure.entity.InspectionEntity;
import com.app.usochicamochabackend.review.infrastructure.repository.InspectionRepository;
import com.app.usochicamochabackend.update.application.dto.*;
import com.app.usochicamochabackend.update.application.port.*;
import com.app.usochicamochabackend.update.infrastructure.entity.OilChangeEntity;
import com.app.usochicamochabackend.update.infrastructure.repository.BrandRepository;
import com.app.usochicamochabackend.update.infrastructure.repository.OilChangeRepository;
// OilChangeStreamController removed - using WebSocket only
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OilChangeService implements
        PerformMotorOilChangeUseCase,
        PerformHydraulicChangeUseCase,
        GetConsolidateMotorOilByIdMachineUseCase,
        GetConsolidateHydraulicOilByIdMachineUseCase,
        GetConsolidateMotorOilAllMachinesUseCase,
        GetConsolidateHydraulicOilAllMachines,
        GetConsolidateHydraulicAndMotorOilByIdMachineUseCase,
        GetConsolidateHydraulicAndMotorOilAllMachinesUseCase
{

    private final MachineRepository machineRepository;
    private final BrandRepository brandRepository;
    private final InspectionRepository inspectionRepository;
    private final OilChangeRepository oilChangeRepository;
    private final SaveActionUseCase saveActionUseCase;
    private final NotificationService notificationService;

    @Override
    public List<ConsolidateHydraulicAndMotorOilDTO> getConsolidateHydraulicAndMotorOilAllMachines() {

        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        saveActionUseCase.save("El usuario " + userPrincipal.username() + " ha obtenido el consolidado de todas la maquinas");
        

        return machineRepository.findAll().stream()
                .filter(MachineEntity::getStatus)
                .map(machine -> getConsolidateHydraulicAndMotorOilById(machine.getId()))
                .filter(Objects::nonNull)
                .toList();
    }


    @Override
    public ConsolidateHydraulicAndMotorOilDTO getConsolidateHydraulicAndMotorOilById(Long machineId) {
        MachineEntity machine = machineRepository.findById(machineId)
                .orElseThrow(() -> new ResourceNotFoundException("Machine not found with id " + machineId));

        InspectionEntity lastInspection = inspectionRepository.getLastInspection(machineId);

        if (lastInspection == null) return null;

        CurrentData currentData = new CurrentData(
                machine.getBelongsTo(),
                machine.getName(),
                lastInspection.getHourMeter(),
                lastInspection.getDateStamp()
        );

        ConsolidateMotorOilDTO motorOil = getConsolidateMotorOilByIdMachine(machineId);
        ConsolidateMotorOilDTO cleanMotorOil = null;
        if (motorOil != null) {
            cleanMotorOil = new ConsolidateMotorOilDTO(
                    null,
                    motorOil.id(),
                    motorOil.type(),
                    motorOil.brand(),
                    motorOil.quantity(),
                    motorOil.averageChangeHours(),
                    motorOil.dateLastUpdate(),
                    motorOil.hourMeterLastUpdate(),
                    motorOil.hourMeterNextUpdate(),
                    motorOil.timeLastUpdateMouths(),
                    motorOil.remainingHoursNextUpdateMouths(),
                    motorOil.status()
            );
        }

        // Hydraulic Oil
        ConsolidateHydraulicOilDTO hydraulicOil = getConsolidateHydraulicOilByIdMachine(machineId);
        ConsolidateHydraulicOilDTO cleanHydraulicOil = null;
        if (hydraulicOil != null) {
            cleanHydraulicOil = new ConsolidateHydraulicOilDTO(
                    null,
                    hydraulicOil.id(),
                    hydraulicOil.type(),
                    hydraulicOil.brand(),
                    hydraulicOil.quantity(),
                    hydraulicOil.averageChangeHours(),
                    hydraulicOil.dateLastUpdate(),
                    hydraulicOil.hourMeterLastUpdate(),
                    hydraulicOil.hourMeterNextUpdate(),
                    hydraulicOil.timeLastUpdateMouths(),
                    hydraulicOil.remainingHoursNextUpdateMouths(),
                    hydraulicOil.status()
            );
        }

        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        saveActionUseCase.save("El usuario " + userPrincipal.username() + " ha obtenido el consolidado de la maquina " + machine.getName());
        

        return new ConsolidateHydraulicAndMotorOilDTO(
                MachineMapper.toResponse(machine),
                currentData,
                cleanMotorOil,
                cleanHydraulicOil
        );
    }

    @Override
    public List<ConsolidateHydraulicOilDTO> getConsolidateHydraulicOilAllMachines() {

        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        saveActionUseCase.save("El usuario " + userPrincipal.username() + " ha obtenido el consolidado de todas las maquinas");
        

        return machineRepository.findAll().stream()
                .map(machine -> getConsolidateHydraulicOilByIdMachine(machine.getId()))
                .toList();
    }

    @Override
    public ConsolidateHydraulicOilDTO getConsolidateHydraulicOilByIdMachine(Long machineId) {
        MachineEntity machine = machineRepository.findById(machineId)
                .orElseThrow(() -> new ResourceNotFoundException("Machine not found with id " + machineId));

        InspectionEntity lastInspection = inspectionRepository.getLastInspection(machineId);

        if (lastInspection == null) return null;

        CurrentData currentData = new CurrentData(
                machine.getBelongsTo(),
                machine.getName(),
                lastInspection.getHourMeter(),
                lastInspection.getDateStamp()
        );

        OilChangeEntity oilLastChange = oilChangeRepository.getLastHydraulicOilChangeByMachineId(machineId);

        if (oilLastChange == null) return null;

        int averageChangeHours = oilLastChange.getAverageHoursChange();
        double hourMeterLastUpdate = oilLastChange.getHourMeter();
        double hourMeterNextUpdate = hourMeterLastUpdate + averageChangeHours;

        double timeLastUpdateMouths = Math.round((ChronoUnit.DAYS.between(
                oilLastChange.getDateStamp(),
                LocalDateTime.now()
        ) / 30.0) * 10.0) / 10.0;

        double remainingHoursNextUpdateMouths = hourMeterNextUpdate - currentData.currentHourMeter();

        String status = null;

        if (currentData.currentHourMeter() <= (hourMeterNextUpdate - 50)) {
            status = "OK";
        } else if (currentData.currentHourMeter() <= hourMeterNextUpdate) {
            status = "Proximo a cambio";
            notificationService.notifyOilChange("Proximo a cambio: " + machine.getName());
        } else {
            status = "Cambio de aceite";
            notificationService.notifyOilChange("Cambio de aceite requerido: " + machine.getName());
        }

        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        saveActionUseCase.save("El usuario " + userPrincipal.username() + " ha obtenido el consolidado de aceite hidraulico de la maquina " + machine.getName());
        

        return new ConsolidateHydraulicOilDTO(
                currentData,
                oilLastChange.getId(),
                "HYDRAULIC",
                oilLastChange.getBrand(),
                oilLastChange.getQuantity(),
                averageChangeHours,
                oilLastChange.getDateStamp().toLocalDate(),
                hourMeterLastUpdate,
                hourMeterNextUpdate,
                timeLastUpdateMouths,
                remainingHoursNextUpdateMouths,
                status
        );
    }

    @Override
    public List<ConsolidateMotorOilDTO> getConsolidateMotorOilAll() {

        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        saveActionUseCase.save("El usuario " + userPrincipal.username() + " ha obtenido el consolidado de aceite de motor de todas las maquinas");
        

        return machineRepository.findAll().stream()
                .map(machine -> getConsolidateMotorOilByIdMachine(machine.getId()))
                .toList();
    }

    @Override
    public ConsolidateMotorOilDTO getConsolidateMotorOilByIdMachine(Long machineId) {
        MachineEntity machine = machineRepository.findById(machineId)
                .orElseThrow(() -> new ResourceNotFoundException("Machine not found with id " + machineId));

        InspectionEntity lastInspection = inspectionRepository.getLastInspection(machineId);
        CurrentData currentData = new CurrentData(
                machine.getBelongsTo(),
                machine.getName(),
                lastInspection.getHourMeter(),
                lastInspection.getDateStamp()
        );

        OilChangeEntity oilLastChange = oilChangeRepository.getLastMotorOilChangeByMachineId(machineId);

        if (oilLastChange == null) return null;

        int averageChangeHours = oilLastChange.getAverageHoursChange();
        
        double hourMeterLastUpdate = oilLastChange.getHourMeter();
        
        double hourMeterNextUpdate = hourMeterLastUpdate + averageChangeHours;

        double timeLastUpdateMouths = Math.round((ChronoUnit.DAYS.between(
                oilLastChange.getDateStamp(),
                LocalDateTime.now()
        ) / 30.0) * 10.0) / 10.0;

        double remainingHoursNextUpdateMouths = hourMeterNextUpdate - currentData.currentHourMeter();

        String status = null;

        if (currentData.currentHourMeter() <= (hourMeterNextUpdate - 50)) {
            status = "OK";
        } else if (currentData.currentHourMeter() <= hourMeterNextUpdate) {
            status = "Proximo a cambio";
            notificationService.notifyOilChange("Proximo a cambio: " + machine.getName());
        } else {
            status = "Cambio de aceite";
            notificationService.notifyOilChange("Cambio de aceite requerido: " + machine.getName());
        }

        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        saveActionUseCase.save("El usuario " + userPrincipal.username() + " ha obtenido el consolidado de aceite de motor de la maquina " + machine.getName());
        

        return new ConsolidateMotorOilDTO(
                currentData,
                oilLastChange.getId(),
                "MOTOR",
                oilLastChange.getBrand(),
                oilLastChange.getQuantity(),
                averageChangeHours,
                oilLastChange.getDateStamp().toLocalDate(),
                oilLastChange.getHourMeter(),
                hourMeterNextUpdate,
                timeLastUpdateMouths,
                remainingHoursNextUpdateMouths,
                status
        );
    }

    @Override
    public PerformChangeMotorOilResponse performMotorOilChange(PerformChangeMotorOilRequest request) {
        OilChangeEntity oilChange = OilChangeMapper.motorOilRequestToEntity(request, machineRepository, brandRepository);

        MachineEntity machine = machineRepository.findById(request.machineId()).orElseThrow(() -> new ResourceNotFoundException("Machine not found with id " + request.machineId()));

        InspectionEntity lastInspection = inspectionRepository.getLastInspection(request.machineId());

        if (request.currentHourMeter() < lastInspection.getHourMeter()) {
            throw new BadRequestException("The stated hour meter cannot be less than the last inspection hour meter");
        }

        oilChangeRepository.save(oilChange);

        machine.setHorometroActual(request.currentHourMeter().intValue());
        machineRepository.save(machine);

        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        saveActionUseCase.save("El usuario " + userPrincipal.username() + " ha cambiado el aceite de motor de la maquina " + machine.getName());


        return OilChangeMapper.motorOilEntityToResponse(oilChange);
    }

    @Override
    public PerformChangeHydraulicOilResponse performChangeHydraulicOil(PerformChangeHydraulicOilRequest request) {
        OilChangeEntity oilChange = OilChangeMapper.hydraulicOilRequestToEntity(request, machineRepository, brandRepository);

        MachineEntity machine = machineRepository.findById(request.machineId()).orElseThrow(() -> new ResourceNotFoundException("Machine not found with id " + request.machineId()));

        InspectionEntity lastInspection = inspectionRepository.getLastInspection(request.machineId());

        if (request.currentHourMeter() < lastInspection.getHourMeter()) {
            throw new BadRequestException("The stated hour meter cannot be less than the last inspection hour meter");
        }

        oilChangeRepository.save(oilChange);

        machine.setHorometroActual(request.currentHourMeter().intValue());
        machineRepository.save(machine);

        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        saveActionUseCase.save("El usuario " + userPrincipal.username() + " ha cambiado el aceite hidraulico de la maquina " + machine.getName());


        return OilChangeMapper.hydraulicOilEntityToResponse(oilChange);
    }
}

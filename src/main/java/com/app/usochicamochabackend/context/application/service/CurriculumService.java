package com.app.usochicamochabackend.context.application.service;

import com.app.usochicamochabackend.actions.application.port.SaveActionUseCase;
import com.app.usochicamochabackend.auth.application.dto.UserPrincipal;
import com.app.usochicamochabackend.context.application.dto.MachineCurriculumDTO;
import com.app.usochicamochabackend.context.application.dto.MachineInspectionRecordDTO;
import com.app.usochicamochabackend.context.application.port.GetMachineCurriculumUseCase;
import com.app.usochicamochabackend.exception.ResourceNotFoundException;
import com.app.usochicamochabackend.machine.application.dto.MachineResponse;
import com.app.usochicamochabackend.machine.application.port.FindMachineByIdUseCase;
import com.app.usochicamochabackend.machine.infrastructure.entity.MachineEntity;
import com.app.usochicamochabackend.machine.infrastructure.repository.MachineRepository;
import com.app.usochicamochabackend.mapper.MachineMapper;
import com.app.usochicamochabackend.mapper.ResultMapper;
import com.app.usochicamochabackend.notifications.application.NotificationService;
import com.app.usochicamochabackend.order.infrastructure.entity.OrderEntity;
import com.app.usochicamochabackend.performance.application.dto.LaborResponse;
import com.app.usochicamochabackend.performance.application.dto.ResultDTO;
import com.app.usochicamochabackend.performance.application.dto.SparePartResponse;
import com.app.usochicamochabackend.performance.infrastructure.entity.ResultEntity;
import com.app.usochicamochabackend.review.infrastructure.entity.InspectionEntity;
import com.app.usochicamochabackend.review.infrastructure.repository.InspectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurriculumService implements GetMachineCurriculumUseCase {

    private final MachineRepository machineRepository;
    private final InspectionRepository inspectionRepository;
    private final FindMachineByIdUseCase findMachineByIdUseCase;
    private final SaveActionUseCase saveActionUseCase;
    private final NotificationService notificationService;

    @Override
    public MachineCurriculumDTO getMachineCurriculum(Long machineId) {
        log.info("🔍 getMachineCurriculum called for machineId: {}", machineId);

        MachineEntity machine = machineRepository.findById(machineId)
                .orElseThrow(() -> new ResourceNotFoundException("Machine not found"));

        List<InspectionEntity> inspectionEntities = inspectionRepository.findByMachineId(machineId);
        log.info("📊 Total inspections found for machineId {}: {}", machineId, inspectionEntities.size());

        if (inspectionEntities.isEmpty()) {
            throw new ResourceNotFoundException("No inspections found");
        }

        // Retornar todas las inspecciones ordenadas por fecha descendente
        List<MachineInspectionRecordDTO> inspectionRecords = inspectionEntities.stream()
                .sorted(Comparator.comparing(InspectionEntity::getDateStamp).reversed())
                .map(inspection -> {
                    List<ResultDTO> resultDTOS = inspection.getOrders().stream()
                            .map(OrderEntity::getResult)
                            .filter(Objects::nonNull)
                            .map(ResultMapper::toResponseResult)
                            .toList();

                    return new MachineInspectionRecordDTO(
                            inspection.getId(),
                            inspection.getDateStamp(),
                            inspection.getHourMeter(),
                            inspection.getLeakStatus(),
                            inspection.getBrakeStatus(),
                            inspection.getBeltsPulleysStatus(),
                            inspection.getTireLanesStatus(),
                            inspection.getCarIgnitionStatus(),
                            inspection.getElectricalStatus(),
                            inspection.getMechanicalStatus(),
                            inspection.getTemperatureStatus(),
                            inspection.getOilStatus(),
                            inspection.getHydraulicStatus(),
                            inspection.getCoolantStatus(),
                            inspection.getStructuralStatus(),
                            inspection.getObservations(),
                            resultDTOS
                    );
                })
                .toList();

        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String username = "anonymous";

            if (principal instanceof UserPrincipal userPrincipal) {
                username = userPrincipal.username();
            }

            saveActionUseCase.save("El usuario " + username +
                    " ha observado el curriculum de la maquina " + machine.getName());
        } catch (Exception e) {
            // If no authentication or error getting principal, use anonymous
            saveActionUseCase.save("Usuario anonymous ha observado el curriculum de la maquina " + machine.getName());
        }

        return new MachineCurriculumDTO(MachineMapper.toResponse(machine), inspectionRecords);
    }

}

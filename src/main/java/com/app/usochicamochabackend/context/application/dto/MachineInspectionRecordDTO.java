package com.app.usochicamochabackend.context.application.dto;

import com.app.usochicamochabackend.performance.application.dto.ResultDTO;

import java.time.LocalDateTime;
import java.util.List;

public record MachineInspectionRecordDTO(
    Long inspectionId,
    LocalDateTime dateStamp,
    Double hourMeter,
    String leakStatus,
    String brakeStatus,
    String beltsPulleysStatus,
    String tireLanesStatus,
    String carIgnitionStatus,
    String electricalStatus,
    String mechanicalStatus,
    String temperatureStatus,
    String oilStatus,
    String hydraulicStatus,
    String coolantStatus,
    String structuralStatus,
    String observations,
    List<ResultDTO> results
) {}

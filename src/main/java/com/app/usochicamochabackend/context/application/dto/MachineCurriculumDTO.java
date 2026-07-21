package com.app.usochicamochabackend.context.application.dto;

import com.app.usochicamochabackend.machine.application.dto.MachineResponse;

import java.util.List;

public record MachineCurriculumDTO(MachineResponse machine, List<MachineInspectionRecordDTO> inspections) {}
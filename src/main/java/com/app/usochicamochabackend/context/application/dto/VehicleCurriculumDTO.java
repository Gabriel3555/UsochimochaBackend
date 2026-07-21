package com.app.usochicamochabackend.context.application.dto;

import com.app.usochicamochabackend.performance.application.dto.ResultDTO;
import com.app.usochicamochabackend.vehicle.application.dto.VehicleResponse;

import java.util.List;

public record VehicleCurriculumDTO(VehicleResponse vehicle, List<ResultDTO> results) {}

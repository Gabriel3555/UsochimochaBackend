package com.app.usochicamochabackend.context.application.port;

import com.app.usochicamochabackend.context.application.dto.VehicleCurriculumDTO;

public interface GetVehicleCurriculumUseCase {
    VehicleCurriculumDTO getVehicleCurriculum(Integer vehicleId);
}

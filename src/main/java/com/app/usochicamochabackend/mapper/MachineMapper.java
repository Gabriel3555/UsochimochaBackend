package com.app.usochicamochabackend.mapper;

import com.app.usochicamochabackend.exception.ResourceNotFoundException;
import com.app.usochicamochabackend.machine.application.dto.MachineResponse;
import com.app.usochicamochabackend.machine.infrastructure.entity.MachineEntity;
import com.app.usochicamochabackend.machine.infrastructure.repository.MachineRepository;

public class MachineMapper {

    private MachineMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static MachineEntity idToEntity(Long machineId, MachineRepository machineRepository) throws ResourceNotFoundException {
        return machineRepository.findById(machineId).orElseThrow(() -> new ResourceNotFoundException("Machine not found with id: " + machineId));
    }

    public static MachineResponse toResponse(MachineEntity entity) {
        if (entity == null) {
            return null;
        }

        return new MachineResponse(
                entity.getId(),
                entity.getName(),
                entity.getBelongsTo(),
                entity.getModel(),
                entity.getSoat(),
                entity.getBrand(),
                entity.getRunt(),
                entity.getNumEngine(),
                entity.getNumInterIdentification()
        );
    }
}
package com.app.usochicamochabackend.exception;

import com.app.usochicamochabackend.vehicle.application.dto.VehicleResponse;
import lombok.Getter;

@Getter
public class VehicleSoftDeletedConflictException extends RuntimeException {
    private final VehicleResponse softDeletedVehicle;
    private final String suggestedAction;

    public VehicleSoftDeletedConflictException(String message, VehicleResponse softDeletedVehicle) {
        super(message);
        this.softDeletedVehicle = softDeletedVehicle;
        this.suggestedAction = "restore_or_create_new";
    }

    public VehicleSoftDeletedConflictException(String message, VehicleResponse softDeletedVehicle, String suggestedAction) {
        super(message);
        this.softDeletedVehicle = softDeletedVehicle;
        this.suggestedAction = suggestedAction;
    }
}

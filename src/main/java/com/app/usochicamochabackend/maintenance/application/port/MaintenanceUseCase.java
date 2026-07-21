package com.app.usochicamochabackend.maintenance.application.port;

import com.app.usochicamochabackend.maintenance.application.dto.MaintenanceRequest;
import com.app.usochicamochabackend.maintenance.application.dto.MaintenanceResponse;
import java.util.List;

public interface MaintenanceUseCase {
    void registerMaintenance(MaintenanceRequest request);
    List<MaintenanceResponse> getMotosMaintenance();
    List<MaintenanceResponse> getVehiclesMaintenance();
}

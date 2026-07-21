package com.app.usochicamochabackend.vehicle.application.port;

import com.app.usochicamochabackend.vehicle.application.dto.VehicleMonitoringDTO;
import java.util.List;

public interface VehicleMonitoringUseCase {
    List<VehicleMonitoringDTO> getConsolidatedMonitoring();
}

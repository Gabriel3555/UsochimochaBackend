package com.app.usochicamochabackend.moto.application.port;

import com.app.usochicamochabackend.moto.application.dto.MotoMonitoringDTO;
import java.util.List;

public interface MotoMonitoringUseCase {
    List<MotoMonitoringDTO> getConsolidatedMonitoring();
}

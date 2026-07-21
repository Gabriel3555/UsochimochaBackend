package com.app.usochicamochabackend.moto.application.port;

import com.app.usochicamochabackend.vehicle.application.dto.VehicleRequest;
import com.app.usochicamochabackend.vehicle.application.dto.VehicleResponse;
import java.util.List;

public interface MotoCRUDUseCase {
    List<VehicleResponse> findAllMotos();
    VehicleResponse createMoto(VehicleRequest request);
    VehicleResponse updateMoto(Integer id, VehicleRequest request);
    void deleteMoto(Integer id);
}

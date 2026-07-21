package com.app.usochicamochabackend.vehicle.application.port;

import com.app.usochicamochabackend.vehicle.application.dto.VehicleRequest;
import com.app.usochicamochabackend.vehicle.application.dto.VehicleResponse;
import java.util.List;

public interface VehicleUseCase {
    List<VehicleResponse> findAllVehicles();
    VehicleResponse findByPlaca(String placa);
    VehicleResponse createVehicle(VehicleRequest request);
    VehicleResponse updateVehicle(Integer id, VehicleRequest request);
    void deleteVehicle(Integer id);
}

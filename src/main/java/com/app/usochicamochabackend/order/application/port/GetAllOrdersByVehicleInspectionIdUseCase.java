package com.app.usochicamochabackend.order.application.port;

import com.app.usochicamochabackend.order.application.dto.GetAllOrdersByVehicleInspectionIdResponse;

public interface GetAllOrdersByVehicleInspectionIdUseCase {
    GetAllOrdersByVehicleInspectionIdResponse getAllOrdersByVehicleInspectionId(Long vehicleInspectionId);
}

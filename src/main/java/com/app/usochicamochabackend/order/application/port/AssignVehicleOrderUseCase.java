package com.app.usochicamochabackend.order.application.port;

import com.app.usochicamochabackend.order.application.dto.AssignVehicleOrderRequest;
import com.app.usochicamochabackend.order.application.dto.OrderWithVehicleDTO;

public interface AssignVehicleOrderUseCase {
    OrderWithVehicleDTO assignVehicleOrder(AssignVehicleOrderRequest request);
}

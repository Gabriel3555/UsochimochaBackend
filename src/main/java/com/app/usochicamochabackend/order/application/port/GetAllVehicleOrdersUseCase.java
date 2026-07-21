package com.app.usochicamochabackend.order.application.port;

import com.app.usochicamochabackend.order.application.dto.OrderWithVehicleDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GetAllVehicleOrdersUseCase {
    Page<OrderWithVehicleDTO> getAllVehicleOrders(Pageable pageable);
}

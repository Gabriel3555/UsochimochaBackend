package com.app.usochicamochabackend.context.application.service;

import com.app.usochicamochabackend.actions.application.port.SaveActionUseCase;
import com.app.usochicamochabackend.auth.application.dto.UserPrincipal;
import com.app.usochicamochabackend.context.application.dto.VehicleCurriculumDTO;
import com.app.usochicamochabackend.context.application.port.GetVehicleCurriculumUseCase;
import com.app.usochicamochabackend.exception.ResourceNotFoundException;
import com.app.usochicamochabackend.mapper.ResultMapper;
import com.app.usochicamochabackend.mapper.VehicleMapper;
import com.app.usochicamochabackend.order.infrastructure.entity.OrderEntity;
import com.app.usochicamochabackend.order.infrastructure.repository.OrderRepository;
import com.app.usochicamochabackend.performance.application.dto.ResultDTO;
import com.app.usochicamochabackend.performance.infrastructure.entity.ResultEntity;
import com.app.usochicamochabackend.vehicle.application.dto.VehicleResponse;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.VehicleEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class VehicleCurriculumService implements GetVehicleCurriculumUseCase {

    private final VehicleRepository vehicleRepository;
    private final OrderRepository orderRepository;
    private final SaveActionUseCase saveActionUseCase;

    @Transactional
    @Override
    public VehicleCurriculumDTO getVehicleCurriculum(Integer vehicleId) {
        VehicleEntity vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with ID: " + vehicleId));

        List<OrderEntity> orders = orderRepository.findAllByVehicleId(vehicleId);

        List<ResultEntity> results = orders.stream()
                .map(OrderEntity::getResult)
                .filter(Objects::nonNull)
                .toList();

        List<ResultDTO> resultDTOs = ResultMapper.toResponseList(results);

        VehicleResponse vehicleResponse = VehicleMapper.toResponse(vehicle);

        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String username = principal instanceof UserPrincipal up ? up.username() : "anonymous";
            saveActionUseCase.save("El usuario " + username +
                    " ha consultado la hoja de vida del vehículo " + vehicle.getPlaca());
        } catch (Exception ignored) {}

        return new VehicleCurriculumDTO(vehicleResponse, resultDTOs);
    }
}

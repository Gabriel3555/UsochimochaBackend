package com.app.usochicamochabackend.order.application.service;

import com.app.usochicamochabackend.actions.application.port.SaveActionUseCase;
import com.app.usochicamochabackend.auth.application.dto.UserPrincipal;
import com.app.usochicamochabackend.auth.infrastructure.entity.UserEntity;
import com.app.usochicamochabackend.auth.infrastructure.repository.UserRepositoryJpa;
import com.app.usochicamochabackend.exception.ResourceNotFoundException;
import com.app.usochicamochabackend.machine.application.dto.MachineResponse;
import com.app.usochicamochabackend.machine.infrastructure.entity.MachineEntity;
import com.app.usochicamochabackend.machine.infrastructure.repository.MachineRepository;
import com.app.usochicamochabackend.mapper.InspectionMapper;
import com.app.usochicamochabackend.mapper.MachineMapper;
import com.app.usochicamochabackend.mapper.OrderMapper;
import com.app.usochicamochabackend.notifications.application.NotificationService;
import com.app.usochicamochabackend.order.application.dto.*;
import com.app.usochicamochabackend.order.application.port.*;
import com.app.usochicamochabackend.order.infrastructure.entity.MaintenanceType;
import com.app.usochicamochabackend.order.infrastructure.entity.OrderCounterEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.VehicleEntity;
import com.app.usochicamochabackend.order.infrastructure.entity.OrderEntity;
import com.app.usochicamochabackend.order.infrastructure.repository.OrderCounterRepository;
import com.app.usochicamochabackend.order.infrastructure.repository.OrderRepository;
import com.app.usochicamochabackend.review.infrastructure.entity.InspectionEntity;
import com.app.usochicamochabackend.review.infrastructure.repository.InspectionRepository;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.entity.InspPreOperativaEntity;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.repository.InspPreOperativaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService implements AssignOrderUseCase, GetAllOrdersByInspectionIdUseCase, GetOrderByIdUseCase,
        GetAllOrdersUseCase, GetAllOrdersByMachineIdUseCase,
        AssignVehicleOrderUseCase, GetAllOrdersByVehicleInspectionIdUseCase, GetAllVehicleOrdersUseCase {

    private final OrderRepository orderRepository;
    private final OrderCounterRepository orderCounterRepository;
    private final MachineRepository machineRepository;
    private final InspectionRepository inspectionRepository;
    private final InspPreOperativaRepository inspPreOperativaRepository;
    private final UserRepositoryJpa userRepository;
    private final SaveActionUseCase saveActionUseCase;
    private final NotificationService notificationService;

    @Transactional
    @Override
    public OrderResponse assignOrder(AssignOrderRequest assignOrderRequest) {
        InspectionEntity inspectionEntity = inspectionRepository.findById(assignOrderRequest.inspectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Inspection not found with ID: " + assignOrderRequest.inspectionId()));

        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long assignerUserId = userPrincipal.id();

        UserEntity assignerUser = userRepository.findById(assignerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Assigner user not found with ID: " + assignerUserId));

        String consecutive = generateConsecutive("MQ");

        MaintenanceType maintenanceType = assignOrderRequest.maintenanceType() != null
                ? MaintenanceType.valueOf(assignOrderRequest.maintenanceType())
                : null;

        OrderEntity orderEntity = orderRepository.save(
                OrderEntity.builder()
                        .status("Pending")
                        .description(assignOrderRequest.description())
                        .assignerUser(assignerUser)
                        .inspection(inspectionEntity)
                        .orderType(assignOrderRequest.orderType())
                        .maintenanceType(maintenanceType)
                        .consecutive(consecutive)
                        .build()
        );

        saveActionUseCase.save("El usuario " + assignerUser.getUsername() +
                " ha asignado una orden de trabajo a la inspección realizada a la maquina " + inspectionEntity.getMachine().getName() + " el dia " + orderEntity.getInspection().getDateStamp());


        return OrderMapper.toDto(orderEntity);
    }

    @Override
    public GetAllOrdersByInspectionIdResponse getAllOrdersByInspectionId(Long inspectionId) {
        InspectionEntity inspectionEntity = inspectionRepository.findById(inspectionId).orElseThrow(() -> new ResourceNotFoundException("Inspection not found with ID: " + inspectionId));

        List<OrderEntity> orders = orderRepository.getAllByInspectionId(inspectionId);
        if (orders.isEmpty()) {
            throw new ResourceNotFoundException("No orders found");
        }

        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        saveActionUseCase.save("El usuario " + userPrincipal.username() +
                " ha observado todas la ordenes de trabajo asigandas a la inspeccion que se le realizo a la maquina " + inspectionEntity.getMachine().getName() + " el dia " + inspectionEntity.getDateStamp().toLocalDate());


        return new GetAllOrdersByInspectionIdResponse(InspectionMapper.toDto(inspectionEntity), orders.stream().map(OrderMapper::toDto).toList());
    }

    @Override
    public OrderResponse getOrderById(Long orderId) {

        return OrderMapper.toDto(orderRepository.findById(orderId).orElseThrow(()->new ResourceNotFoundException("Order not found with ID: " + orderId)));
    }

    @Override
    public Page<OrderWithMachineDTO> getAllOrders(Pageable pageable) {
        Page<OrderEntity> orders = orderRepository.findAllByInspectionIsNotNull(pageable);

        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        saveActionUseCase.save("El usuario " + userPrincipal.username() +
                " ha observado todas la ordenes de trabajo asigandas");

        return orders.map(order -> {
            OrderWithoutInspectionResponse orderDTO = OrderMapper.toDtoWithoutInspection(order);
            MachineResponse machineDTO = null;
            if (order.getInspection() != null && order.getInspection().getMachine() != null) {
                machineDTO = MachineMapper.toResponse(order.getInspection().getMachine());
            }
            return new OrderWithMachineDTO(orderDTO, machineDTO);
        });
    }

    @Transactional
    @Override
    public OrderWithVehicleDTO assignVehicleOrder(AssignVehicleOrderRequest request) {
        InspPreOperativaEntity vehicleInspection = inspPreOperativaRepository.findById(request.vehicleInspectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle inspection not found with ID: " + request.vehicleInspectionId()));

        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserEntity assignerUser = userRepository.findById(userPrincipal.id())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userPrincipal.id()));

        VehicleEntity vehiculo = vehicleInspection.getVehiculo();
        String tipoNombre = (vehiculo != null && vehiculo.getTipoVehiculo() != null)
                ? vehiculo.getTipoVehiculo().getNombreTipo().toUpperCase(java.util.Locale.ROOT)
                : "";
        String moduleCode = tipoNombre.contains("MOTO") ? "MT" : "VH";
        String consecutive = generateConsecutive(moduleCode);

        MaintenanceType maintenanceType = request.maintenanceType() != null
                ? MaintenanceType.valueOf(request.maintenanceType())
                : null;

        OrderEntity order = orderRepository.save(
                OrderEntity.builder()
                        .status("Pending")
                        .description(request.description())
                        .assignerUser(assignerUser)
                        .vehicleInspection(vehicleInspection)
                        .orderType(request.orderType())
                        .maintenanceType(maintenanceType)
                        .consecutive(consecutive)
                        .build()
        );

        String placa = vehiculo != null ? vehiculo.getPlaca() : "";
        saveActionUseCase.save("El usuario " + assignerUser.getUsername() +
                " ha asignado una orden de trabajo a la inspección del vehículo " + placa +
                " del día " + vehicleInspection.getFechaRegistro());

        return OrderMapper.toVehicleOrderDTO(order);
    }

    @Transactional
    @Override
    public GetAllOrdersByVehicleInspectionIdResponse getAllOrdersByVehicleInspectionId(Long vehicleInspectionId) {
        InspPreOperativaEntity vehicleInspection = inspPreOperativaRepository.findById(vehicleInspectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle inspection not found with ID: " + vehicleInspectionId));

        List<OrderEntity> orders = orderRepository.getAllByVehicleInspectionId(vehicleInspectionId);

        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String placa = vehicleInspection.getVehiculo() != null ? vehicleInspection.getVehiculo().getPlaca() : "";
        saveActionUseCase.save("El usuario " + userPrincipal.username() +
                " ha consultado las órdenes de la inspección del vehículo " + placa);

        return new GetAllOrdersByVehicleInspectionIdResponse(
                vehicleInspectionId,
                placa,
                vehicleInspection.getFechaRegistro(),
                OrderMapper.toDtoListWithoutInspection(orders)
        );
    }

    @Transactional
    @Override
    public Page<OrderWithVehicleDTO> getAllVehicleOrders(Pageable pageable) {
        Page<OrderEntity> orders = orderRepository.findAllByVehicleInspectionIsNotNull(pageable);

        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        saveActionUseCase.save("El usuario " + userPrincipal.username() +
                " ha consultado todas las órdenes de vehículos");

        return orders.map(OrderMapper::toVehicleOrderDTO);
    }

    @Override
    public GetAllOrdersByMachineId getAllOrdersByMachineId(Long machineId) {
        MachineEntity machineEntity = machineRepository.findById(machineId).orElseThrow(()->new ResourceNotFoundException("Machine not found with ID: " + machineId));

        List<InspectionEntity> inspections = inspectionRepository.findByMachineId(machineId);

        if (inspections.isEmpty()) {
            throw new ResourceNotFoundException("No inspections found");
        }

        List<OrderEntity> orders = inspections.stream()
                .flatMap(inspection -> inspection.getOrders().stream())
                .toList();

        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        saveActionUseCase.save("El usuario " + userPrincipal.username() +
                " ha observado todas la ordenes de trabajo asigandas a la maquina " + machineEntity.getName());


        return new GetAllOrdersByMachineId(MachineMapper.toResponse(machineEntity), OrderMapper.toDtoListWithoutInspection(orders));
    }

    @Transactional
    protected String generateConsecutive(String moduleCode) {
        OrderCounterEntity counter = orderCounterRepository.findByModuleCodeForUpdate(moduleCode)
                .orElse(new OrderCounterEntity(moduleCode, 0));
        int newValue = counter.getLastValue() + 1;
        counter.setLastValue(newValue);
        orderCounterRepository.save(counter);
        return "OT-" + moduleCode + "-" + String.format("%05d", newValue);
    }
}

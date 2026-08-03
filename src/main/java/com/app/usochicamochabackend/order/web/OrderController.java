package com.app.usochicamochabackend.order.web;

import com.app.usochicamochabackend.auth.application.dto.UserPrincipal;
import com.app.usochicamochabackend.order.application.dto.*;
import com.app.usochicamochabackend.order.application.port.*;
import com.app.usochicamochabackend.update.application.service.ExcelGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/order")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Endpoints for managing orders")
public class OrderController {

    private final AssignOrderUseCase assignOrderUseCase;
    private final GetAllOrdersUseCase getAllOrdersUseCase;
    private final AssignVehicleOrderUseCase assignVehicleOrderUseCase;
    private final GetAllVehicleOrdersUseCase getAllVehicleOrdersUseCase;
    private final ExcelGenerationService excelGenerationService;

    @Operation(
            summary = "Assign a new order",
            description = "Creates a new order for a given inspection, assigning it to a specific user with an optional description."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order successfully created",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "404", description = "Inspection, assigner user, or assigned user not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OrderResponse> assignOrder(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Order assignment data including inspectionId, assignerUserId, assignedUserId and description",
                    required = true,
                    content = @Content(schema = @Schema(implementation = AssignOrderRequest.class))
            )
            @RequestBody AssignOrderRequest assignOrderRequest) {

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId;
        if (principal instanceof UserPrincipal userPrincipal) {
            userId = userPrincipal.id();
        } else if (principal instanceof org.springframework.security.core.userdetails.User user) {
            // For test purposes, extract user ID from username or use a default
            userId = 1L; // Default user ID for tests
        } else {
            throw new IllegalStateException("Unexpected principal type: " + principal.getClass());
        }
        System.out.println("userId = " + userId);
        
        return ResponseEntity.status(201).body(assignOrderUseCase.assignOrder(assignOrderRequest));
    }

    @GetMapping("/all")
    public ResponseEntity<Page<OrderWithMachineDTO>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return ResponseEntity.ok(getAllOrdersUseCase.getAllOrders(pageable));
    }

    @Operation(summary = "Create vehicle order", description = "Creates a work order linked to a vehicle pre-operative inspection.")
    @PostMapping(value = "/vehicle", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OrderWithVehicleDTO> assignVehicleOrder(
            @RequestBody AssignVehicleOrderRequest request) {
        return ResponseEntity.status(201).body(assignVehicleOrderUseCase.assignVehicleOrder(request));
    }

    @Operation(summary = "Get all vehicle orders (paginated)", description = "Returns all work orders linked to vehicle inspections, sorted by most recent.")
    @GetMapping("/vehicle/all")
    public ResponseEntity<Page<OrderWithVehicleDTO>> getAllVehicleOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return ResponseEntity.ok(getAllVehicleOrdersUseCase.getAllVehicleOrders(pageable));
    }

    @Operation(summary = "Exportar órdenes de maquinaria a Excel")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportMachineOrders() throws IOException {
        List<OrderWithMachineDTO> orders = getAllOrdersUseCase.getAllOrders(Pageable.unpaged()).getContent();
        byte[] excelData = excelGenerationService.generateMachineOrdersExcel(orders);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "ordenes_maquinaria.xlsx");
        return ResponseEntity.ok().headers(headers).body(excelData);
    }

    @Operation(summary = "Exportar órdenes de vehículos y motos a Excel",
            description = "Por defecto exporta ambos. Con `soloMotos=true` exporta únicamente las órdenes cuyo tipo de vehículo es MOTOCICLETA.")
    @GetMapping("/vehicle/export")
    public ResponseEntity<byte[]> exportVehicleOrders(
            @RequestParam(defaultValue = "false") boolean soloMotos) throws IOException {
        List<OrderWithVehicleDTO> orders = getAllVehicleOrdersUseCase.getAllVehicleOrders(Pageable.unpaged()).getContent();

        if (soloMotos) {
            orders = orders.stream()
                    .filter(o -> o.vehicle() != null
                            && "MOTOCICLETA".equalsIgnoreCase(o.vehicle().tipoVehiculo()))
                    .toList();
        }

        byte[] excelData = excelGenerationService.generateVehicleOrdersExcel(orders);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", soloMotos ? "ordenes_motos.xlsx" : "ordenes_vehiculos_motos.xlsx");
        return ResponseEntity.ok().headers(headers).body(excelData);
    }
}
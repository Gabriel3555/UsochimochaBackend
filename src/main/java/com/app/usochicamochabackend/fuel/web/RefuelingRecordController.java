package com.app.usochicamochabackend.fuel.web;

import com.app.usochicamochabackend.auth.application.dto.UserPrincipal;
import com.app.usochicamochabackend.fuel.application.dto.RefuelingRecordRequest;
import com.app.usochicamochabackend.fuel.application.dto.RefuelingRecordResponse;
import com.app.usochicamochabackend.fuel.application.port.RegisterRefuelingRecordUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/fuel/refueling")
@RequiredArgsConstructor
public class RefuelingRecordController {

    private final RegisterRefuelingRecordUseCase registerRefuelingRecordUseCase;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OPERARIO', 'ALMACEN', 'SUPERVISOR_OPERATIVO', 'ADMIN')")
    public ResponseEntity<RefuelingRecordResponse> registrar(
            @RequestPart(value = "vehicleId", required = false) String vehicleId,
            @RequestPart(value = "machineId", required = false) String machineId,
            @RequestPart("lugar") String lugar,
            @RequestPart("areaCosto") String areaCosto,
            @RequestPart("fuelTypeId") String fuelTypeId,
            @RequestPart("cantidadGalones") String cantidadGalones,
            @RequestPart("horometroKm") String horometroKm,
            @RequestPart(value = "esFull", required = false) String esFull,
            @RequestPart(value = "precioUnitario", required = false) String precioUnitario,
            @RequestPart(value = "descuento", required = false) String descuento,
            @RequestPart(value = "totalIngresado", required = false) String totalIngresado,
            @RequestPart(value = "origen", required = false) String origen,
            @RequestPart(value = "factura", required = false) MultipartFile factura) {
        RefuelingRecordRequest request = new RefuelingRecordRequest(
                vehicleId != null && !vehicleId.isBlank() ? Integer.valueOf(vehicleId) : null,
                machineId != null && !machineId.isBlank() ? Long.valueOf(machineId) : null,
                lugar,
                areaCosto,
                Long.valueOf(fuelTypeId),
                new BigDecimal(cantidadGalones),
                new BigDecimal(horometroKm),
                esFull != null && Boolean.parseBoolean(esFull),
                precioUnitario != null && !precioUnitario.isBlank() ? new BigDecimal(precioUnitario) : null,
                descuento != null && !descuento.isBlank() ? new BigDecimal(descuento) : null,
                totalIngresado != null && !totalIngresado.isBlank() ? new BigDecimal(totalIngresado) : null,
                origen);

        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        RefuelingRecordResponse response = registerRefuelingRecordUseCase.registrar(request, factura, userPrincipal.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OPERARIO', 'ALMACEN', 'SUPERVISOR_OPERATIVO', 'ADMIN')")
    public ResponseEntity<Page<RefuelingRecordResponse>> listar(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) Boolean activo) {
        return ResponseEntity.ok(registerRefuelingRecordUseCase.listar(pageable, activo));
    }
}

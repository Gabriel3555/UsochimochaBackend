package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.actions.application.port.SaveActionUseCase;
import com.app.usochicamochabackend.fuel.application.dto.RefuelingRecordRequest;
import com.app.usochicamochabackend.fuel.application.dto.RefuelingRecordResponse;
import com.app.usochicamochabackend.fuel.application.port.AdjustFuelInventoryUseCase;
import com.app.usochicamochabackend.fuel.application.port.RegisterRefuelingRecordUseCase;
import com.app.usochicamochabackend.fuel.infrastructure.entity.RefuelingRecordsEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.RefuelingRecordsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RefuelingRecordService implements RegisterRefuelingRecordUseCase {

    private static final String LUGAR_BOMBA = "BOMBA";
    private static final String LUGAR_ALMACEN = "ALMACEN";

    private final RefuelingRecordsRepository refuelingRecordsRepository;
    private final AdjustFuelInventoryUseCase adjustFuelInventoryUseCase;
    private final FuelDocumentStorageService fuelDocumentStorageService;
    private final SaveActionUseCase saveActionUseCase;

    @Value("${app.fuel.discrepancia-tolerancia-porcentaje:0.01}")
    private BigDecimal tolerancia;

    @Override
    @Transactional
    public RefuelingRecordResponse registrar(RefuelingRecordRequest request, MultipartFile factura, Long responsableId) {
        boolean tieneVehiculo = request.vehicleId() != null;
        boolean tieneMaquina = request.machineId() != null;
        if (tieneVehiculo == tieneMaquina) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe indicar exactamente uno de vehicleId o machineId.");
        }
        if (request.lugar() == null || (!request.lugar().equals(LUGAR_BOMBA) && !request.lugar().equals(LUGAR_ALMACEN))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lugar debe ser BOMBA o ALMACEN.");
        }
        if (request.areaCosto() == null || request.fuelTypeId() == null || request.cantidadGalones() == null
                || request.horometroKm() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "areaCosto, fuelTypeId, cantidadGalones y horometroKm son obligatorios.");
        }

        BigDecimal totalCalculado = null;
        Boolean discrepanciaValor = false;
        boolean esBomba = request.lugar().equals(LUGAR_BOMBA);

        if (esBomba) {
            if (request.precioUnitario() == null || factura == null || factura.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Un tanqueo en BOMBA requiere precioUnitario y factura.");
            }
            if (request.totalIngresado() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "totalIngresado es obligatorio en BOMBA.");
            }
            BigDecimal descuento = request.descuento() != null ? request.descuento() : BigDecimal.ZERO;
            totalCalculado = request.cantidadGalones().multiply(request.precioUnitario()).subtract(descuento);
            discrepanciaValor = totalCalculado.subtract(request.totalIngresado()).abs()
                    .compareTo(totalCalculado.multiply(tolerancia)) > 0;
        } else {
            // ALMACEN: valida y descuenta stock ANTES de persistir (lanza 409 si no alcanza).
            adjustFuelInventoryUseCase.decrement(request.areaCosto(), request.fuelTypeId(), request.cantidadGalones());
        }

        RefuelingRecordsEntity entity = RefuelingRecordsEntity.builder()
                .vehicleId(request.vehicleId())
                .machineId(request.machineId())
                .lugar(request.lugar())
                .areaCosto(request.areaCosto())
                .fuelTypeId(request.fuelTypeId())
                .cantidadGalones(request.cantidadGalones())
                .horometroKm(request.horometroKm())
                .esFull(request.esFull() != null ? request.esFull() : false)
                .precioUnitario(request.precioUnitario())
                .descuento(request.descuento())
                .totalIngresado(request.totalIngresado())
                .totalCalculado(totalCalculado)
                .discrepanciaValor(discrepanciaValor)
                .urlFactura(null)
                .origen(request.origen())
                .responsableId(responsableId)
                .fechaRegistro(Timestamp.valueOf(LocalDateTime.now()))
                .status(true)
                .build();
        entity = refuelingRecordsRepository.save(entity);

        if (esBomba) {
            try {
                String urlFactura = fuelDocumentStorageService.store(factura, "refueling", entity.getId());
                entity.setUrlFactura(urlFactura);
                entity = refuelingRecordsRepository.save(entity);
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar la factura.", e);
            }
        }

        String activo = tieneVehiculo ? ("vehículo id=" + request.vehicleId()) : ("máquina id=" + request.machineId());
        saveActionUseCase.save("Se registró un tanqueo de " + request.cantidadGalones()
                + " galones en " + request.lugar() + " para " + activo);

        return mapToResponse(entity);
    }

    @Override
    public Page<RefuelingRecordResponse> listar(Pageable pageable, Boolean activo) {
        Page<RefuelingRecordsEntity> page = activo != null
                ? refuelingRecordsRepository.findByStatus(activo, pageable)
                : refuelingRecordsRepository.findAll(pageable);
        return page.map(this::mapToResponse);
    }

    private RefuelingRecordResponse mapToResponse(RefuelingRecordsEntity entity) {
        return new RefuelingRecordResponse(
                entity.getId(),
                entity.getVehicleId(),
                entity.getMachineId(),
                entity.getLugar(),
                entity.getAreaCosto(),
                entity.getFuelTypeId(),
                entity.getCantidadGalones(),
                entity.getHorometroKm(),
                entity.getEsFull(),
                entity.getPrecioUnitario(),
                entity.getDescuento(),
                entity.getTotalIngresado(),
                entity.getTotalCalculado(),
                entity.getDiscrepanciaValor(),
                entity.getUrlFactura(),
                entity.getOrigen(),
                entity.getResponsableId(),
                entity.getFechaRegistro() != null ? entity.getFechaRegistro().toLocalDateTime() : null
        );
    }
}

package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.actions.application.port.SaveActionUseCase;
import com.app.usochicamochabackend.fuel.application.dto.FuelPurchaseRequest;
import com.app.usochicamochabackend.fuel.application.dto.FuelPurchaseResponse;
import com.app.usochicamochabackend.fuel.application.port.AdjustFuelInventoryUseCase;
import com.app.usochicamochabackend.fuel.application.port.RegisterFuelPurchaseUseCase;
import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelPurchaseEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelPurchaseRepository;
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
public class FuelPurchaseService implements RegisterFuelPurchaseUseCase {

    private final FuelPurchaseRepository fuelPurchaseRepository;
    private final AdjustFuelInventoryUseCase adjustFuelInventoryUseCase;
    private final FuelDocumentStorageService fuelDocumentStorageService;
    private final SaveActionUseCase saveActionUseCase;

    @Value("${app.fuel.discrepancia-tolerancia-porcentaje:0.01}")
    private BigDecimal tolerancia;

    @Override
    @Transactional
    public FuelPurchaseResponse registrar(FuelPurchaseRequest request, MultipartFile factura, Long responsableId) {
        if (request.areaCosto() == null || request.fuelTypeId() == null
                || request.cantidad() == null || request.precioUnitario() == null
                || request.totalIngresado() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "areaCosto, fuelTypeId, cantidad, precioUnitario y totalIngresado son obligatorios.");
        }
        if (factura == null || factura.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La factura es obligatoria.");
        }

        BigDecimal descuento = request.descuento() != null ? request.descuento() : BigDecimal.ZERO;
        BigDecimal totalCalculado = request.cantidad().multiply(request.precioUnitario()).subtract(descuento);
        boolean discrepanciaValor = totalCalculado.subtract(request.totalIngresado()).abs()
                .compareTo(totalCalculado.multiply(tolerancia)) > 0;

        FuelPurchaseEntity entity = FuelPurchaseEntity.builder()
                .areaCosto(request.areaCosto())
                .fuelTypeId(request.fuelTypeId())
                .cantidad(request.cantidad())
                .precioUnitario(request.precioUnitario())
                .descuento(descuento)
                .totalIngresado(request.totalIngresado())
                .totalCalculado(totalCalculado)
                .discrepanciaValor(discrepanciaValor)
                .urlFactura("pendiente")
                .responsableId(responsableId)
                .fechaCompra(Timestamp.valueOf(LocalDateTime.now()))
                .status(true)
                .build();
        entity = fuelPurchaseRepository.save(entity);

        String urlFactura;
        try {
            urlFactura = fuelDocumentStorageService.store(factura, "purchases", entity.getId());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar la factura.", e);
        }
        entity.setUrlFactura(urlFactura);
        entity = fuelPurchaseRepository.save(entity);

        adjustFuelInventoryUseCase.increment(request.areaCosto(), request.fuelTypeId(), request.cantidad());

        saveActionUseCase.save("Se registró una compra de " + request.cantidad()
                + " galones de combustible (tipo id=" + request.fuelTypeId()
                + ") para el área " + request.areaCosto());

        return mapToResponse(entity);
    }

    @Override
    public Page<FuelPurchaseResponse> listar(Pageable pageable) {
        return fuelPurchaseRepository.findAll(pageable).map(this::mapToResponse);
    }

    private FuelPurchaseResponse mapToResponse(FuelPurchaseEntity entity) {
        return new FuelPurchaseResponse(
                entity.getId(),
                entity.getAreaCosto(),
                entity.getFuelTypeId(),
                entity.getCantidad(),
                entity.getPrecioUnitario(),
                entity.getDescuento(),
                entity.getTotalIngresado(),
                entity.getTotalCalculado(),
                entity.getDiscrepanciaValor(),
                entity.getUrlFactura(),
                entity.getResponsableId(),
                entity.getFechaCompra() != null ? entity.getFechaCompra().toLocalDateTime() : null
        );
    }
}

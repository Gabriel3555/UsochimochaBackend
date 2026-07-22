package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.actions.application.port.SaveActionUseCase;
import com.app.usochicamochabackend.exception.ResourceNotFoundException;
import com.app.usochicamochabackend.fuel.application.dto.FuelReintegrationRequest;
import com.app.usochicamochabackend.fuel.application.dto.FuelReintegrationResponse;
import com.app.usochicamochabackend.fuel.application.port.AdjustFuelInventoryUseCase;
import com.app.usochicamochabackend.fuel.application.port.RegisterFuelReintegrationUseCase;
import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelReintegrationsEntity;
import com.app.usochicamochabackend.fuel.infrastructure.entity.RefuelingRecordsEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelReintegrationsRepository;
import com.app.usochicamochabackend.fuel.infrastructure.repository.RefuelingRecordsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FuelReintegrationService implements RegisterFuelReintegrationUseCase {

    private static final String LUGAR_ALMACEN = "ALMACEN";

    private final FuelReintegrationsRepository fuelReintegrationsRepository;
    private final RefuelingRecordsRepository refuelingRecordsRepository;
    private final AdjustFuelInventoryUseCase adjustFuelInventoryUseCase;
    private final SaveActionUseCase saveActionUseCase;

    @Override
    @Transactional
    public FuelReintegrationResponse registrar(FuelReintegrationRequest request, Long responsableId) {
        if (request.refuelingId() == null || request.cantidadReintegrada() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "refuelingId y cantidadReintegrada son obligatorios.");
        }

        RefuelingRecordsEntity tanqueo = refuelingRecordsRepository.findById(request.refuelingId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe el tanqueo con id=" + request.refuelingId()));

        if (request.cantidadReintegrada().compareTo(tanqueo.getCantidadGalones()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La cantidad reintegrada no puede superar la cantidad tanqueada originalmente.");
        }

        // Decisión confirmada: los tanqueos ALMACEN no se valorizan (no tienen precio propio),
        // pero sí devuelven la cantidad al inventario. Los BOMBA valorizan con su propio precio
        // y no tocan inventario (no hay inventario propio de por medio en BOMBA).
        boolean esAlmacen = LUGAR_ALMACEN.equals(tanqueo.getLugar());
        java.math.BigDecimal valorReintegro = esAlmacen || tanqueo.getPrecioUnitario() == null
                ? null
                : request.cantidadReintegrada().multiply(tanqueo.getPrecioUnitario());

        if (esAlmacen) {
            adjustFuelInventoryUseCase.increment(tanqueo.getAreaCosto(), tanqueo.getFuelTypeId(), request.cantidadReintegrada());
        }

        FuelReintegrationsEntity entity = FuelReintegrationsEntity.builder()
                .refuelingId(tanqueo.getId())
                .cantidadReintegrada(request.cantidadReintegrada())
                .valorReintegro(valorReintegro)
                .responsableId(responsableId)
                .fechaReintegro(Timestamp.valueOf(LocalDateTime.now()))
                .build();
        entity = fuelReintegrationsRepository.save(entity);

        saveActionUseCase.save("Se registró un reintegro de " + request.cantidadReintegrada()
                + " galones para el tanqueo id=" + request.refuelingId());

        return mapToResponse(entity);
    }

    private FuelReintegrationResponse mapToResponse(FuelReintegrationsEntity entity) {
        return new FuelReintegrationResponse(
                entity.getId(),
                entity.getRefuelingId(),
                entity.getCantidadReintegrada(),
                entity.getValorReintegro(),
                entity.getResponsableId(),
                entity.getFechaReintegro() != null ? entity.getFechaReintegro().toLocalDateTime() : null);
    }
}

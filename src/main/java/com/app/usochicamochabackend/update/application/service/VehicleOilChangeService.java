package com.app.usochicamochabackend.update.application.service;

import com.app.usochicamochabackend.common.text.InputTextNormalizer;
import com.app.usochicamochabackend.notifications.application.PreventiveAlertCalculationService;
import com.app.usochicamochabackend.update.application.dto.VehicleOilChangeHistoryDTO;
import com.app.usochicamochabackend.update.application.dto.VehicleOilChangeRequest;
import com.app.usochicamochabackend.update.application.exception.BrandNotFoundException;
import com.app.usochicamochabackend.update.application.exception.VehicleNotFoundException;
import com.app.usochicamochabackend.update.infrastructure.entity.BrandEntity;
import com.app.usochicamochabackend.update.infrastructure.entity.VehicleOilChangeEntity;
import com.app.usochicamochabackend.update.infrastructure.entity.OilType;
import com.app.usochicamochabackend.update.infrastructure.repository.BrandRepository;
import com.app.usochicamochabackend.update.infrastructure.repository.VehicleOilChangeRepository;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.VehicleEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleOilChangeService {

    private static final Logger logger = LoggerFactory.getLogger(VehicleOilChangeService.class);

    private final VehicleOilChangeRepository vehicleOilChangeRepository;
    private final VehicleRepository vehicleRepository;
    private final BrandRepository brandRepository;
    private final OilChangeValidationService validationService;
    private final PreventiveAlertCalculationService preventiveAlertCalculationService;

    @Transactional
    public void registerChange(VehicleOilChangeRequest request) {
        logger.info("Iniciando registro de cambio de aceite para vehículo: {}", request.placa());

        // Validar datos de entrada
        validationService.validateOilChangeRequest(request);

        try {
            String placa = InputTextNormalizer.normalizePlaca(request.placa());

            // Obtener vehículo
            VehicleEntity vehicle = vehicleRepository.findByPlaca(placa)
                    .orElseThrow(() -> {
                        logger.error("Vehículo no encontrado con placa: {}", placa);
                        return new VehicleNotFoundException(placa);
                    });

            logger.debug("Vehículo encontrado: {} (ID: {})", placa, vehicle.getIdVehiculo());

            // Obtener marca si se proporciona
            BrandEntity brand = null;
            if (request.brandId() != null) {
                brand = brandRepository.findById(request.brandId())
                        .orElseThrow(() -> {
                            logger.error("Marca no encontrada con ID: {}", request.brandId());
                            return new BrandNotFoundException(request.brandId());
                        });
                logger.debug("Marca encontrada: {}", brand.getName());
            }

            // Crear entidad de cambio de aceite
            VehicleOilChangeEntity entity = VehicleOilChangeEntity.builder()
                    .vehicle(vehicle)
                    .dateStamp(request.dateStamp() != null ? request.dateStamp() : java.time.LocalDateTime.now())
                    .oilType(OilType.fromString(request.oilType()))
                    .brand(brand)
                    .quantity(request.quantity())
                    .kmAtChange(request.kmAtChange())
                    .intervalKm(request.intervalKm())
                    .airFilterChanged(request.airFilterChanged())
                    .build();

            // Guardar cambio de aceite
            vehicleOilChangeRepository.save(entity);
            logger.debug("Cambio de aceite guardado: {} km, {} km intervalo",
                request.kmAtChange(), request.intervalKm());

            // Actualizar vehículo
            Integer currentKm = vehicle.getKilometrajeActual() != null ? vehicle.getKilometrajeActual() : 0;
            boolean needsUpdate = false;

            if (request.kmAtChange() != null && request.kmAtChange() > currentKm) {
                vehicle.setKilometrajeActual(request.kmAtChange());
                needsUpdate = true;
                logger.debug("Km actualizado: {} -> {}", currentKm, request.kmAtChange());
            }

            vehicle.setFechaUltimoReporte(java.time.LocalDateTime.now());
            needsUpdate = true;

            if (needsUpdate) {
                vehicleRepository.save(vehicle);
                logger.debug("Vehículo actualizado con km y fecha de reporte");
            }

            // Recalcular alertas de inmediato: este cambio de aceite resetea la línea base,
            // así que la alerta previa (si existía) debe desaparecer/actualizarse ya mismo.
            preventiveAlertCalculationService.calculateAndEmitAlerts();

            logger.info("Cambio de aceite registrado exitosamente para: {}", placa);

        } catch (VehicleNotFoundException | BrandNotFoundException e) {
            logger.error("Error de validación al registrar cambio de aceite: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error inesperado al registrar cambio de aceite para placa: {}",
                request.placa(), e);
            throw new RuntimeException("Error al registrar cambio de aceite: " + e.getMessage(), e);
        }
    }

    public List<VehicleOilChangeHistoryDTO> getHistoryByPlaca(String placa) {
        logger.debug("Obteniendo historial de cambios de aceite para: {}", placa);

        String p = InputTextNormalizer.normalizePlaca(placa);
        List<VehicleOilChangeEntity> history = vehicleOilChangeRepository.findAllByPlacaOrderByDateStampDesc(p);

        if (history.isEmpty()) {
            logger.warn("Sin historial de cambios de aceite para: {}", placa);
        } else {
            logger.debug("Se encontraron {} cambios de aceite para: {}", history.size(), placa);
        }

        return history.stream()
                .map(e -> new VehicleOilChangeHistoryDTO(
                        e.getId(),
                        e.getDateStamp(),
                        e.getBrand() != null ? e.getBrand().getName() : null,
                        e.getQuantity(),
                        e.getKmAtChange(),
                        e.getIntervalKm(),
                        e.getAirFilterChanged()
                ))
                .toList();
    }
}

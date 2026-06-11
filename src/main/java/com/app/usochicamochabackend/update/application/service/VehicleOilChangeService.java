package com.app.usochicamochabackend.update.application.service;

import com.app.usochicamochabackend.common.text.InputTextNormalizer;
import com.app.usochicamochabackend.update.application.dto.VehicleOilChangeHistoryDTO;
import com.app.usochicamochabackend.update.application.dto.VehicleOilChangeRequest;
import com.app.usochicamochabackend.update.infrastructure.entity.BrandEntity;
import com.app.usochicamochabackend.update.infrastructure.entity.VehicleOilChangeEntity;
import com.app.usochicamochabackend.update.infrastructure.entity.OilType;
import com.app.usochicamochabackend.update.infrastructure.repository.BrandRepository;
import com.app.usochicamochabackend.update.infrastructure.repository.VehicleOilChangeRepository;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.VehicleEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleOilChangeService {

    private final VehicleOilChangeRepository vehicleOilChangeRepository;
    private final VehicleRepository vehicleRepository;
    private final BrandRepository brandRepository;

    @Transactional
    public void registerChange(VehicleOilChangeRequest request) {
        String placa = InputTextNormalizer.normalizePlaca(request.placa());
        if (placa.isEmpty()) {
            throw new IllegalArgumentException("La placa es obligatoria");
        }
        VehicleEntity vehicle = vehicleRepository.findByPlaca(placa)
                .orElseThrow(() -> new IllegalArgumentException("Vehículo no encontrado"));

        BrandEntity brand = null;
        if (request.brandId() != null) {
            brand = brandRepository.findById(request.brandId())
                    .orElseThrow(() -> new IllegalArgumentException("Marca de aceite no encontrada"));
        }

        VehicleOilChangeEntity entity = VehicleOilChangeEntity.builder()
                .vehicle(vehicle)
                .dateStamp(request.dateStamp() != null ? request.dateStamp() : java.time.LocalDateTime.now())
                .oilType(OilType.valueOf(request.oilType().toUpperCase().replace("-", "_").replace(" ", "_")))
                .brand(brand)
                .quantity(request.quantity())
                .kmAtChange(request.kmAtChange())
                .intervalKm(request.intervalKm())
                .airFilterChanged(request.airFilterChanged())
                .build();

        vehicleOilChangeRepository.save(entity);

        // Actualizar kilometraje si es necesario y fecha del último reporte
        Integer kmChange = request.kmAtChange();
        boolean needsUpdate = false;

        if (kmChange != null && kmChange > (vehicle.getKilometrajeActual() != null ? vehicle.getKilometrajeActual() : 0)) {
            vehicle.setKilometrajeActual(kmChange);
            needsUpdate = true;
        }

        // Actualizar fecha del último reporte (cambio de aceite es una actividad reportada)
        vehicle.setFechaUltimoReporte(java.time.LocalDateTime.now());
        needsUpdate = true;

        if (needsUpdate) {
            vehicleRepository.save(vehicle);
        }
    }

    public List<VehicleOilChangeHistoryDTO> getHistoryByPlaca(String placa) {
        String p = InputTextNormalizer.normalizePlaca(placa);
        return vehicleOilChangeRepository.findAllByPlacaOrderByDateStampDesc(p)
                .stream()
                .map(e -> new VehicleOilChangeHistoryDTO(
                        e.getId(),
                        e.getDateStamp(),
                        e.getOilType() != null ? e.getOilType().name() : null,
                        e.getBrand() != null ? e.getBrand().getName() : null,
                        e.getQuantity(),
                        e.getKmAtChange(),
                        e.getIntervalKm(),
                        e.getAirFilterChanged()
                ))
                .toList();
    }
}

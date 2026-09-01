package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.actions.application.port.SaveActionUseCase;
import com.app.usochicamochabackend.exception.ResourceNotFoundException;
import com.app.usochicamochabackend.fuel.application.dto.FuelMonthlyDiscountRequest;
import com.app.usochicamochabackend.fuel.application.dto.FuelMonthlyDiscountResponse;
import com.app.usochicamochabackend.fuel.application.port.ManageFuelMonthlyDiscountUseCase;
import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelMonthlyDiscountEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelMonthlyDiscountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FuelMonthlyDiscountService implements ManageFuelMonthlyDiscountUseCase {

    private final FuelMonthlyDiscountRepository fuelMonthlyDiscountRepository;
    private final SaveActionUseCase saveActionUseCase;

    @Override
    @Transactional
    public FuelMonthlyDiscountResponse registrar(FuelMonthlyDiscountRequest request, Long responsableId) {
        validar(request);

        FuelMonthlyDiscountEntity entity = FuelMonthlyDiscountEntity.builder()
                .fechaInicio(request.fechaInicio())
                .fechaFin(request.fechaFin())
                .monto(request.monto())
                .responsableId(responsableId)
                .fechaRegistro(Timestamp.valueOf(LocalDateTime.now()))
                .status(true)
                .build();
        entity = fuelMonthlyDiscountRepository.save(entity);

        saveActionUseCase.save("Se registró un descuento mensual de combustible de " + request.monto()
                + " para el periodo " + request.fechaInicio() + " a " + request.fechaFin());
        return FuelMonthlyDiscountResponse.from(entity);
    }

    @Override
    public List<FuelMonthlyDiscountResponse> listar() {
        return fuelMonthlyDiscountRepository.findByStatusTrueOrderByFechaInicioDesc().stream()
                .map(FuelMonthlyDiscountResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        FuelMonthlyDiscountEntity entity = fuelMonthlyDiscountRepository.findById(id)
                .filter(FuelMonthlyDiscountEntity::getStatus)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el descuento mensual con id=" + id));

        entity.setStatus(false);
        fuelMonthlyDiscountRepository.save(entity);

        saveActionUseCase.save("Se eliminó el descuento mensual de combustible id=" + id);
    }

    private void validar(FuelMonthlyDiscountRequest request) {
        if (request.fechaInicio() == null || request.fechaFin() == null || request.monto() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "fechaInicio, fechaFin y monto son obligatorios.");
        }
        if (request.fechaFin().isBefore(request.fechaInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fechaFin no puede ser anterior a fechaInicio.");
        }
        if (request.monto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "monto debe ser mayor a 0.");
        }
    }
}

package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.fuel.application.dto.FuelDistributionResponse;
import com.app.usochicamochabackend.fuel.application.port.GetFuelDistributionUseCase;
import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelReintegrationsEntity;
import com.app.usochicamochabackend.fuel.infrastructure.entity.RefuelingRecordsEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelReintegrationsRepository;
import com.app.usochicamochabackend.fuel.infrastructure.repository.RefuelingRecordsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FuelDistributionService implements GetFuelDistributionUseCase {

    private static final String LUGAR_BOMBA = "BOMBA";

    private final RefuelingRecordsRepository refuelingRecordsRepository;
    private final FuelReintegrationsRepository fuelReintegrationsRepository;

    private static final String AREA_TODAS = "TODAS";

    @Override
    public FuelDistributionResponse obtenerDistribucion(String area, LocalDate fechaInicio, LocalDate fechaFin) {
        boolean todas = area == null || AREA_TODAS.equals(area);
        if (!todas && !"DISTRITO".equals(area) && !"ASOCIACION".equals(area)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "area debe ser DISTRITO, ASOCIACION o TODAS.");
        }
        FechaRangoUtil.Rango rango = FechaRangoUtil.resolver(fechaInicio, fechaFin);

        List<RefuelingRecordsEntity> tanqueos = todas
                ? refuelingRecordsRepository.findByFechaRegistroBetween(rango.inicio(), rango.fin())
                : refuelingRecordsRepository.findByAreaCostoAndFechaRegistroBetween(area, rango.inicio(), rango.fin());

        List<FuelDistributionResponse.Fila> filas = tanqueos.stream().map(this::mapFila).toList();

        BigDecimal totalGalones = filas.stream()
                .map(FuelDistributionResponse.Fila::cantidadDespachada)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCosto = filas.stream()
                .map(FuelDistributionResponse.Fila::valorDespachado)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new FuelDistributionResponse(rango.fechaInicio(), rango.fechaFin(), todas ? AREA_TODAS : area, totalGalones, totalCosto, filas);
    }

    private FuelDistributionResponse.Fila mapFila(RefuelingRecordsEntity tanqueo) {
        // Decisión confirmada: los tanqueos ALMACEN no tienen precio propio y no se valorizan.
        BigDecimal valorDespachado = LUGAR_BOMBA.equals(tanqueo.getLugar()) && tanqueo.getPrecioUnitario() != null
                ? tanqueo.getCantidadGalones().multiply(tanqueo.getPrecioUnitario())
                : null;

        List<FuelReintegrationsEntity> reintegros = fuelReintegrationsRepository.findByRefuelingId(tanqueo.getId());
        BigDecimal cantidadReintegrada = null;
        BigDecimal valorReintegro = null;
        if (!reintegros.isEmpty()) {
            cantidadReintegrada = reintegros.stream()
                    .map(FuelReintegrationsEntity::getCantidadReintegrada)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            valorReintegro = reintegros.stream()
                    .map(FuelReintegrationsEntity::getValorReintegro)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        return new FuelDistributionResponse.Fila(
                tanqueo.getId(),
                tanqueo.getFechaRegistro() != null ? tanqueo.getFechaRegistro().toLocalDateTime() : null,
                tanqueo.getVehicleId(),
                tanqueo.getMachineId(),
                tanqueo.getResponsableId(),
                tanqueo.getFuelTypeId(),
                tanqueo.getOrigen(),
                tanqueo.getHorometroKm(),
                tanqueo.getCantidadGalones(),
                valorDespachado,
                cantidadReintegrada,
                valorReintegro,
                tanqueo.getLugar(),
                tanqueo.getAreaCosto(),
                tanqueo.getEsFull(),
                tanqueo.getPrecioUnitario(),
                tanqueo.getDiscrepanciaValor());
    }
}

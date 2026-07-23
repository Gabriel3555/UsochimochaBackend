package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.fuel.application.dto.FuelWarehouseBalanceResponse;
import com.app.usochicamochabackend.fuel.application.dto.FuelWarehouseMovementsResponse;
import com.app.usochicamochabackend.fuel.application.port.GetFuelWarehouseUseCase;
import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelInventoryEntity;
import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelPurchaseEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelInventoryRepository;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelPurchaseRepository;
import com.app.usochicamochabackend.fuel.infrastructure.repository.RefuelingRecordsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FuelWarehouseService implements GetFuelWarehouseUseCase {

    private final FuelInventoryRepository fuelInventoryRepository;
    private final FuelPurchaseRepository fuelPurchaseRepository;
    private final RefuelingRecordsRepository refuelingRecordsRepository;

    private record AreaTipo(String areaCosto, Long fuelTypeId) {}

    @Override
    public FuelWarehouseBalanceResponse obtenerSaldos() {
        Set<Long> tiposEnUso = tiposEnUso();
        List<FuelWarehouseBalanceResponse.Saldo> saldos = fuelInventoryRepository.findAll().stream()
                .filter(entity -> tiposEnUso.contains(entity.getFuelTypeId()))
                .map(entity -> new FuelWarehouseBalanceResponse.Saldo(
                        entity.getAreaCosto(), entity.getFuelTypeId(), entity.getCantidadDisponible()))
                .toList();
        return new FuelWarehouseBalanceResponse(saldos);
    }

    /**
     * Tipos de combustible con al menos una compra o tanqueo alguna vez — fuel_inventory
     * se siembra para los 4 tipos del catálogo desde el inicio, los maneje o no la
     * organización, así que sin este filtro Saldos/Conciliación siempre muestran los 4
     * aunque solo usen 2 (contenido "basura" del catálogo sin datos reales).
     */
    private Set<Long> tiposEnUso() {
        Set<Long> tipos = new HashSet<>(fuelPurchaseRepository.findDistinctFuelTypeIds());
        tipos.addAll(refuelingRecordsRepository.findDistinctFuelTypeIds());
        return tipos;
    }

    @Override
    public FuelWarehouseMovementsResponse obtenerMovimientos(LocalDate fechaInicio, LocalDate fechaFin) {
        FechaRangoUtil.Rango rango = FechaRangoUtil.resolver(fechaInicio, fechaFin);
        Set<Long> tiposEnUso = tiposEnUso();

        Map<AreaTipo, BigDecimal> saldoActual = new HashMap<>();
        for (FuelInventoryEntity entity : fuelInventoryRepository.findAll()) {
            if (!tiposEnUso.contains(entity.getFuelTypeId())) continue;
            saldoActual.put(new AreaTipo(entity.getAreaCosto(), entity.getFuelTypeId()), entity.getCantidadDisponible());
        }

        Map<AreaTipo, BigDecimal> entradas = new HashMap<>();
        for (Object[] fila : fuelPurchaseRepository.sumCantidadPorAreaYTipoBetween(rango.inicio(), rango.fin())) {
            entradas.put(new AreaTipo((String) fila[0], (Long) fila[1]), (BigDecimal) fila[2]);
        }

        Map<AreaTipo, BigDecimal> salidas = new HashMap<>();
        for (Object[] fila : refuelingRecordsRepository.sumCantidadAlmacenPorAreaYTipoBetween(rango.inicio(), rango.fin())) {
            salidas.put(new AreaTipo((String) fila[0], (Long) fila[1]), (BigDecimal) fila[2]);
        }

        List<FuelWarehouseMovementsResponse.Conciliacion> conciliacion = new ArrayList<>();
        for (Map.Entry<AreaTipo, BigDecimal> entry : saldoActual.entrySet()) {
            AreaTipo key = entry.getKey();
            BigDecimal actual = entry.getValue();
            BigDecimal entrada = entradas.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal salida = salidas.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal saldoInicial = actual.subtract(entrada).add(salida);
            conciliacion.add(new FuelWarehouseMovementsResponse.Conciliacion(
                    key.areaCosto(), key.fuelTypeId(), saldoInicial, entrada, salida, actual));
        }

        List<FuelWarehouseMovementsResponse.CompraHistorial> historial =
                fuelPurchaseRepository.findByFechaCompraBetweenOrderByFechaCompraDesc(rango.inicio(), rango.fin()).stream()
                        .map(this::mapCompraHistorial)
                        .toList();

        return new FuelWarehouseMovementsResponse(rango.fechaInicio(), rango.fechaFin(), conciliacion, historial);
    }

    private FuelWarehouseMovementsResponse.CompraHistorial mapCompraHistorial(FuelPurchaseEntity entity) {
        return new FuelWarehouseMovementsResponse.CompraHistorial(
                entity.getId(),
                entity.getFechaCompra() != null ? entity.getFechaCompra().toLocalDateTime() : null,
                entity.getAreaCosto(),
                entity.getFuelTypeId(),
                entity.getCantidad(),
                entity.getPrecioUnitario(),
                entity.getDescuento(),
                entity.getTotalCalculado(),
                entity.getUrlFactura());
    }
}

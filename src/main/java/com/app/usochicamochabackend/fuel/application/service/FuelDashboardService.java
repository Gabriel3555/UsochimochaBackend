package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.fuel.application.dto.FuelDashboardResponse;
import com.app.usochicamochabackend.fuel.application.port.GetFuelDashboardUseCase;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelPurchaseRepository;
import com.app.usochicamochabackend.fuel.infrastructure.repository.RefuelingRecordsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FuelDashboardService implements GetFuelDashboardUseCase {

    private final FuelPurchaseRepository fuelPurchaseRepository;
    private final RefuelingRecordsRepository refuelingRecordsRepository;

    @Override
    public FuelDashboardResponse obtenerDashboard(LocalDate fechaInicio, LocalDate fechaFin) {
        FechaRangoUtil.Rango rango = FechaRangoUtil.resolver(fechaInicio, fechaFin);

        BigDecimal totalComprasAlmacen = fuelPurchaseRepository.sumTotalCalculadoBetween(rango.inicio(), rango.fin());
        BigDecimal totalDescuentos = fuelPurchaseRepository.sumDescuentoBetween(rango.inicio(), rango.fin());
        BigDecimal totalTanqueosBomba = refuelingRecordsRepository.sumTotalCalculadoBombaBetween(rango.inicio(), rango.fin());

        BigDecimal gastoBruto = totalComprasAlmacen.add(totalTanqueosBomba);
        BigDecimal gastoNeto = gastoBruto.subtract(totalDescuentos);
        BigDecimal ahorro = totalDescuentos;

        List<FuelDashboardResponse.GalonesPorTipo> galonesPorTipo = agruparGalonesPorTipo(rango);

        return new FuelDashboardResponse(
                rango.fechaInicio(), rango.fechaFin(),
                totalComprasAlmacen, totalTanqueosBomba, totalDescuentos,
                gastoBruto, gastoNeto, ahorro,
                galonesPorTipo);
    }

    private List<FuelDashboardResponse.GalonesPorTipo> agruparGalonesPorTipo(FechaRangoUtil.Rango rango) {
        Map<Long, BigDecimal> galonesPorTipo = new LinkedHashMap<>();
        for (Object[] fila : refuelingRecordsRepository.sumCantidadPorTipoBetween(rango.inicio(), rango.fin())) {
            galonesPorTipo.put((Long) fila[0], (BigDecimal) fila[1]);
        }
        return galonesPorTipo.entrySet().stream()
                .map(entry -> new FuelDashboardResponse.GalonesPorTipo(entry.getKey(), entry.getValue()))
                .toList();
    }
}

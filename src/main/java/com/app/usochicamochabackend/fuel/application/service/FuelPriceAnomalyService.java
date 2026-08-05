package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.fuel.infrastructure.repository.RefuelingRecordsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Discrepancia por precio unitario fuera de rango: compara el precio de un tanqueo
 * en BOMBA contra el promedio reciente del mismo combustible — un desvío grande
 * suele ser un dígito de más/menos al digitar, no una variación real de precio.
 * Compartido por cualquier consumidor de tanqueos (creación, edición, reportes),
 * mismo patrón que {@link AssetFuelCapacityService}.
 */
@Service
@RequiredArgsConstructor
public class FuelPriceAnomalyService {

    private final RefuelingRecordsRepository refuelingRecordsRepository;

    @Value("${app.fuel.precio-ventana-dias:30}")
    private int ventanaDias;

    @Value("${app.fuel.precio-unitario-desviacion-tolerancia-porcentaje:0.30}")
    private BigDecimal tolerancia;

    /**
     * @param excludeId id del propio tanqueo a excluir del promedio (al editar uno
     *                  ya existente); null al registrar uno nuevo.
     */
    public boolean precioFueraDeRango(Long fuelTypeId, BigDecimal precioUnitario, Long excludeId) {
        if (precioUnitario == null) {
            return false;
        }
        BigDecimal promedio = promedioReciente(fuelTypeId, excludeId);
        if (promedio == null) {
            return false;
        }
        BigDecimal diferencia = precioUnitario.subtract(promedio).abs();
        return diferencia.compareTo(promedio.multiply(tolerancia)) > 0;
    }

    /**
     * Promedio de precio unitario reciente (BOMBA, mismo combustible) usado como
     * línea base — null si no hay historial suficiente para comparar. Expuesto por
     * separado de {@link #precioFueraDeRango} para que los consumidores puedan
     * mostrar contra qué valor se comparó, no solo el booleano.
     *
     * @param excludeId id del propio tanqueo a excluir del promedio (al editar uno
     *                  ya existente); null al registrar uno nuevo.
     */
    public BigDecimal promedioReciente(Long fuelTypeId, Long excludeId) {
        if (fuelTypeId == null) {
            return null;
        }
        Timestamp desde = Timestamp.valueOf(LocalDateTime.now().minusDays(ventanaDias));
        BigDecimal promedio = refuelingRecordsRepository.avgPrecioUnitarioBombaRecienteByFuelType(
                fuelTypeId, desde, excludeId);
        // Sin historial reciente para comparar (primer tanqueo de ese combustible,
        // o todos fuera de la ventana) — no hay línea base.
        return (promedio == null || promedio.compareTo(BigDecimal.ZERO) <= 0) ? null : promedio;
    }
}

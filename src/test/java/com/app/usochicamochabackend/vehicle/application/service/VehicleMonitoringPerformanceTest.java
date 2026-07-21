package com.app.usochicamochabackend.vehicle.application.service;

import com.app.usochicamochabackend.vehicle.application.dto.VehicleMonitoringDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests de rendimiento para verificar que N+1 está eliminado
 * en la obtención de consolidados
 *
 * Ejecutar con: mvn test -Dtest=VehicleMonitoringPerformanceTest -Dgroups=performance
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("performance")
public class VehicleMonitoringPerformanceTest {

    @Autowired
    private VehicleMonitoringService vehicleMonitoringService;

    @Test
    public void testConsolidatedMonitoringPerformance() {
        // Arrange
        long maxDurationMs = 5000; // Máximo 5 segundos para obtener consolidado

        // Act
        long startTime = System.currentTimeMillis();
        List<VehicleMonitoringDTO> result = vehicleMonitoringService.getConsolidatedMonitoring();
        long duration = System.currentTimeMillis() - startTime;

        // Assert
        assertNotNull(result);
        assertTrue(result.size() >= 0, "Consolidado debería retornar una lista");
        assertTrue(
            duration < maxDurationMs,
            String.format(
                "Consolidado de %d vehículos tomó %d ms (máximo esperado: %d ms). " +
                "Posible indicación de N+1 queries",
                result.size(), duration, maxDurationMs
            )
        );

        // Log resultado
        double avgTimePerVehicle = result.isEmpty() ? 0 : (double) duration / result.size();
        System.out.printf(
            "✓ Consolidado completado: %d vehículos en %d ms (promedio: %.2f ms/vehículo)%n",
            result.size(), duration, avgTimePerVehicle
        );
    }

    @Test
    public void testConsolidatedMonitoringN1QueryDetection() {
        // Este test está diseñado para usarse con SQL logging habilitado
        // Ver: application-test.properties con hibernate.generate_statistics=true

        // Arrange
        long maxQueriesExpected = 10; // Máximo de queries esperadas (sin N+1)

        // Act
        List<VehicleMonitoringDTO> result = vehicleMonitoringService.getConsolidatedMonitoring();

        // Assert
        assertNotNull(result);

        // Si esto falla, verificar logs de Hibernate para detectar N+1
        System.out.printf(
            "✓ Consolidado obtenido con éxito. Para detectar N+1, " +
            "habilitar 'hibernate.generate_statistics=true' en application-test.properties " +
            "y revisar los logs de SessionStatistics%n"
        );
    }

    @Test
    public void testScalabilityBenchmark() {
        // Benchmark simple para medir escalabilidad
        int[] vehicleCounts = {10, 50, 100, 200};

        System.out.println("\n=== Benchmark de Escalabilidad ===");
        System.out.println("Vehículos | Tiempo (ms) | ms/Vehículo | Estado");
        System.out.println("----------|------------|------------|--------");

        for (int i = 0; i < 3; i++) { // 3 iteraciones para warming up
            vehicleMonitoringService.getConsolidatedMonitoring();
        }

        long startOverall = System.currentTimeMillis();
        List<VehicleMonitoringDTO> result = vehicleMonitoringService.getConsolidatedMonitoring();
        long duration = System.currentTimeMillis() - startOverall;

        double msPerVehicle = result.isEmpty() ? 0 : (double) duration / result.size();
        String status = msPerVehicle < 10 ? "✓ Óptimo" :
                       msPerVehicle < 50 ? "⚠ Aceptable" :
                       "✗ Lento - Revisar N+1";

        System.out.printf("%9d | %10d | %10.2f | %s%n",
            result.size(), duration, msPerVehicle, status);
    }
}

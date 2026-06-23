package com.app.usochicamochabackend.notifications.web;

import com.app.usochicamochabackend.notifications.application.PreventiveAlertCalculationService;
import com.app.usochicamochabackend.notifications.application.dto.PreventiveAlertDTO;
import com.app.usochicamochabackend.notifications.infrastructure.repository.PreventiveAlertRepository;
import com.app.usochicamochabackend.update.infrastructure.repository.OilChangeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@Slf4j
public class AlertsController {

    private final PreventiveAlertRepository alertRepository;
    private final PreventiveAlertCalculationService preventiveAlertCalculationService;
    private final OilChangeRepository oilChangeRepository;

    /**
     * GET /api/v1/alerts - Obtener alertas preventivas con paginación y filtros
     *
     * @param page número de página (default: 0)
     * @param size tamaño de página (default: 50)
     * @param estado filtro: ACTIVA o RESUELTA (optional)
     * @return Page de PreventiveAlertDTO
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR_OPERATIVO', 'OPERARIO')")
    public ResponseEntity<Page<PreventiveAlertDTO>> getAllAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String estado
    ) {
        log.debug("📊 Obteniendo alertas - página: {}, tamaño: {}, estado: {}", page, size, estado);

        Pageable pageable = PageRequest.of(page, size, Sort.by("fechaCreacion").descending());

        Page<PreventiveAlertDTO> alerts;
        if (estado != null && !estado.isEmpty()) {
            alerts = alertRepository.findByEstadoAndStatus(estado, true, pageable)
                    .map(PreventiveAlertDTO::fromEntity);
        } else {
            alerts = alertRepository.findByStatus(true, pageable)
                    .map(PreventiveAlertDTO::fromEntity);
        }

        log.info("✅ Retornando {} alertas (página {}/{})",
                alerts.getNumberOfElements(),
                page,
                alerts.getTotalPages());

        return ResponseEntity.ok(alerts);
    }

    /**
     * GET /api/v1/alerts/{id} - Obtener una alerta específica
     *
     * @param id ID de la alerta
     * @return PreventiveAlertDTO
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR_OPERATIVO', 'OPERARIO')")
    public ResponseEntity<PreventiveAlertDTO> getAlert(@PathVariable Long id) {
        log.debug("📋 Obteniendo alerta con ID: {}", id);

        return alertRepository.findById(id)
                .map(alert -> {
                    log.info("✅ Alerta encontrada: {}", alert.getId());
                    return ResponseEntity.ok(PreventiveAlertDTO.fromEntity(alert));
                })
                .orElseGet(() -> {
                    log.warn("⚠️ Alerta no encontrada: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * GET /api/v1/alerts/by-asset/{assetId} - Obtener alertas por activo
     *
     * @param assetId ID del activo (placa vehículo, ID máquina, etc)
     * @param page número de página (default: 0)
     * @param size tamaño de página (default: 20)
     * @return Page de PreventiveAlertDTO
     */
    @GetMapping("/by-asset/{assetId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR_OPERATIVO', 'OPERARIO')")
    public ResponseEntity<Page<PreventiveAlertDTO>> getAlertsByAsset(
            @PathVariable String assetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.debug("🔍 Obteniendo alertas del activo: {}", assetId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("fechaCreacion").descending());
        Page<PreventiveAlertDTO> alerts = alertRepository.findByAssetIdAndStatus(assetId, true, pageable)
                .map(PreventiveAlertDTO::fromEntity);

        log.info("✅ {} alertas encontradas para activo: {}", alerts.getTotalElements(), assetId);

        return ResponseEntity.ok(alerts);
    }

    /**
     * GET /api/v1/alerts/count - Obtener conteo de alertas activas por color
     *
     * @return JSON con conteos: {amarillo: X, rojo: Y, verde: Z, total: W}
     */
    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR_OPERATIVO', 'OPERARIO')")
    public ResponseEntity<AlertCountDTO> getAlertCounts() {
        log.debug("📊 Obteniendo conteo de alertas");

        long amarillo = alertRepository.countByColorEstadoAndStatus("AMARILLO", true);
        long rojo = alertRepository.countByColorEstadoAndStatus("ROJO", true);
        long verde = alertRepository.countByColorEstadoAndStatus("VERDE", true);

        AlertCountDTO counts = new AlertCountDTO(amarillo, rojo, verde, amarillo + rojo + verde);

        log.info("✅ Conteo: AMARILLO={}, ROJO={}, VERDE={}, TOTAL={}",
                amarillo, rojo, verde, counts.total());

        return ResponseEntity.ok(counts);
    }

    /**
     * DELETE /api/v1/alerts/cleanup - Limpia alertas antiguas con IDs numéricos de máquinas
     * Llamar antes de recalcular si las máquinas tienen alertas con IDs en lugar de nombres.
     *
     * @return Mensaje indicando cuántas alertas se limpiaron
     */
    @DeleteMapping("/cleanup")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR_OPERATIVO', 'OPERARIO')")
    public ResponseEntity<CleanupResultDTO> cleanupOldAlerts() {
        log.info("🧹 Limpiando alertas antiguas con IDs numéricos...");

        try {
            // Eliminar alertas de máquinas que tienen assetId puramente numérico
            // (las nuevas alertas tienen nombres como "Excavadora 1")
            var machineAlerts = alertRepository.findByAlertType("OIL_CHANGE_MACHINE", PageRequest.of(0, Integer.MAX_VALUE))
                    .getContent();

            long deletedCount = 0;
            for (var alert : machineAlerts) {
                if (alert.getAssetId() != null && alert.getAssetId().matches("\\d+")) {
                    alertRepository.delete(alert);
                    deletedCount++;
                }
            }

            log.info("✅ {} alertas antiguas de máquinas eliminadas", deletedCount);

            return ResponseEntity.ok(new CleanupResultDTO(true, deletedCount + " alertas eliminadas"));
        } catch (Exception e) {
            log.error("❌ Error limpiando alertas: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(
                    new CleanupResultDTO(false, "Error: " + e.getMessage())
            );
        }
    }

    /**
     * POST /api/v1/alerts/refresh - Recalcula y emite alertas (llamado al abrir página/login)
     * El frontend puede llamar esto cuando abre la página o después del login.
     *
     * @return Mensaje indicando que el cálculo fue completado
     */
    @PostMapping("/refresh")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR_OPERATIVO', 'OPERARIO')")
    public ResponseEntity<CalculationResultDTO> refreshAlerts() {
        log.info("🔄 Refrescando alertas para usuario autenticado...");

        try {
            long startTime = System.currentTimeMillis();
            preventiveAlertCalculationService.calculateAndEmitAlerts();
            long duration = System.currentTimeMillis() - startTime;

            long totalAlerts = alertRepository.count();
            long activeAlerts = alertRepository.findByStatus(true, PageRequest.of(0, Integer.MAX_VALUE))
                    .getTotalElements();

            CalculationResultDTO result = new CalculationResultDTO(
                    true,
                    "Alertas refrescadas exitosamente",
                    duration,
                    totalAlerts,
                    activeAlerts
            );

            log.info("✅ Alertas refrescadas en {}ms. Total: {}, Activas: {}",
                    duration, totalAlerts, activeAlerts);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("❌ Error al refrescar alertas: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(
                    new CalculationResultDTO(false, "Error: " + e.getMessage(), 0, 0, 0)
            );
        }
    }

    /**
     * POST /api/v1/alerts/calculate - Dispara manualmente el cálculo de alertas (TESTING/ADMIN)
     * Esto es útil en desarrollo para verificar que el sistema funciona sin esperar al scheduler.
     *
     * @return Mensaje indicando que el cálculo fue completado
     */
    @PostMapping("/calculate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CalculationResultDTO> manuallyCalculateAlerts() {
        log.info("🔄 [ADMIN] Disparando cálculo manual de alertas...");

        try {
            long startTime = System.currentTimeMillis();
            preventiveAlertCalculationService.calculateAndEmitAlerts();
            long duration = System.currentTimeMillis() - startTime;

            long totalAlerts = alertRepository.count();
            long activeAlerts = alertRepository.findByStatus(true, PageRequest.of(0, Integer.MAX_VALUE))
                    .getTotalElements();

            CalculationResultDTO result = new CalculationResultDTO(
                    true,
                    "Cálculo de alertas completado exitosamente",
                    duration,
                    totalAlerts,
                    activeAlerts
            );

            log.info("✅ Cálculo completado en {}ms. Total alertas: {}, Activas: {}",
                    duration, totalAlerts, activeAlerts);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("❌ Error durante cálculo manual de alertas: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(
                    new CalculationResultDTO(false, "Error: " + e.getMessage(), 0, 0, 0)
            );
        }
    }

    /**
     * DELETE /api/v1/alerts/{id} - Elimina una alerta específica por ID
     * NOTA: Debe estar DESPUÉS de /cleanup y /refresh para evitar conflicto de rutas
     *
     * @param id ID de la alerta
     * @return Respuesta indicando éxito
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR_OPERATIVO', 'OPERARIO')")
    public ResponseEntity<Void> deleteAlert(@PathVariable Long id) {
        log.debug("🗑️ Eliminando alerta con ID: {}", id);

        try {
            alertRepository.deleteById(id);
            log.info("✅ Alerta {} eliminada", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("❌ Error eliminando alerta {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * DTO para respuesta de conteos
     */
    public record AlertCountDTO(long amarillo, long rojo, long verde, long total) {}

    /**
     * DTO para respuesta de cálculo manual
     */
    public record CalculationResultDTO(
            boolean success,
            String message,
            long durationMs,
            long totalAlerts,
            long activeAlerts
    ) {}

    /**
     * DTO para respuesta de limpieza
     */
    public record CleanupResultDTO(boolean success, String message) {}

    /**
     * GET /api/v1/alerts/debug/oil-changes - DIAGNÓSTICO: Ver datos de cambios de aceite en BD
     */
    @GetMapping("/debug/oil-changes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> debugOilChanges() {
        log.info("🔍 [DEBUG] Consultando oil_changes en BD...");
        try {
            var allOilChanges = oilChangeRepository.findAll();

            StringBuilder result = new StringBuilder();
            result.append("=== DIAGNÓSTICO DE OIL CHANGES ===\n\n");
            result.append("Total registros en tabla oil_changes: ").append(allOilChanges.size()).append("\n\n");

            if (allOilChanges.isEmpty()) {
                result.append("⚠️ NO HAY REGISTROS DE OIL_CHANGES EN LA BD\n");
                return ResponseEntity.ok(result.toString());
            }

            result.append("Desglose por máquina y tipo de aceite:\n");
            for (var oilChange : allOilChanges) {
                String machineName = oilChange.getMachine() != null ? oilChange.getMachine().getName() : "UNKNOWN";
                String oilType = oilChange.getOilType() != null ? oilChange.getOilType().toString() : "NULL";

                result.append("  - Máquina: ").append(machineName)
                      .append(", Tipo: ").append(oilType)
                      .append(", Fecha: ").append(oilChange.getDateStamp())
                      .append(", HourStamp: ").append(oilChange.getHourStamp())
                      .append(", Intervalo: ").append(oilChange.getAverageHoursChange())
                      .append("\n");
            }

            return ResponseEntity.ok(result.toString());
        } catch (Exception e) {
            log.error("❌ Error en debug: {}", e.getMessage(), e);
            return ResponseEntity.ok("Error: " + e.getMessage());
        }
    }
}

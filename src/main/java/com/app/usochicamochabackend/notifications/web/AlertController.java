package com.app.usochicamochabackend.notifications.web;

import com.app.usochicamochabackend.notifications.application.AlertSchedulerService;
import com.app.usochicamochabackend.notifications.application.dto.AlertDTO;
import com.app.usochicamochabackend.notifications.infrastructure.entity.AlertEntity;
import com.app.usochicamochabackend.notifications.infrastructure.entity.OilChangeAlertEntity;
import com.app.usochicamochabackend.notifications.infrastructure.repository.AlertRepository;
import com.app.usochicamochabackend.notifications.infrastructure.repository.OilChangeAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertRepository alertRepository;
    private final OilChangeAlertRepository oilChangeAlertRepository;
    private final AlertSchedulerService alertSchedulerService;

    /**
     * Obtener todas las alertas activas con paginación y filtros
     * GET /api/v1/alerts?page=0&size=20&tipo=CAMBIO_ACEITE&estado=ACTIVE
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR_OPERATIVO')")
    public ResponseEntity<Map<String, Object>> getAllAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String placa
    ) {
        log.info("🔔 GET /alerts - Listando alertas (page={}, size={})", page, size);

        // Obtener todas las alertas y filtrar en memoria (simple pero funcional)
        List<AlertEntity> allAlerts = alertRepository.findAll();

        // Aplicar filtros
        List<AlertEntity> filtered = allAlerts.stream()
                .filter(a -> tipo == null || a.getTipoAlerta().equals(tipo))
                .filter(a -> estado == null || a.getEstado().equals(estado))
                .filter(a -> placa == null || a.getPlaca().equals(placa))
                .sorted((a1, a2) -> a2.getFechaCreacion().compareTo(a1.getFechaCreacion())) // Ordenar desc por fecha
                .collect(Collectors.toList());

        // Paginar
        int totalElements = filtered.size();
        int start = page * size;
        int end = Math.min(start + size, totalElements);
        List<AlertDTO> content = filtered.subList(start, end).stream()
                .map(AlertDTO::fromEntity)
                .collect(Collectors.toList());

        Page<AlertDTO> pageResult = new PageImpl<>(content, PageRequest.of(page, size), totalElements);

        Map<String, Object> response = new HashMap<>();
        response.put("content", pageResult.getContent());
        response.put("totalElements", pageResult.getTotalElements());
        response.put("totalPages", pageResult.getTotalPages());
        response.put("currentPage", page);
        response.put("pageSize", size);

        return ResponseEntity.ok(response);
    }

    /**
     * Obtener alertas de una placa específica
     * GET /api/v1/alerts/placa/ABC123
     */
    @GetMapping("/placa/{placa}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR_OPERATIVO')")
    public ResponseEntity<List<AlertDTO>> getAlertsByPlaca(@PathVariable String placa) {
        log.info("🔔 GET /alerts/placa/{} - Listando alertas para placa", placa);

        List<AlertDTO> alerts = alertRepository.findByPlacaOrderByFechaCreacionDesc(placa).stream()
                .map(AlertDTO::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(alerts);
    }

    /**
     * Obtener alerta por ID
     * GET /api/v1/alerts/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR_OPERATIVO')")
    public ResponseEntity<AlertDTO> getAlertById(@PathVariable String id) {
        log.info("🔔 GET /alerts/{} - Obteniendo alerta", id);

        try {
            Long longId = Long.parseLong(id);
            Optional<AlertEntity> alert = alertRepository.findById(longId);
            if (alert.isEmpty()) {
                log.warn("⚠️ Alerta no encontrada: {}", id);
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(AlertDTO.fromEntity(alert.get()));
        } catch (NumberFormatException e) {
            log.warn("⚠️ ID no es un número: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Resolver/cerrar una alerta
     * PATCH /api/v1/alerts/{id}/resolve
     */
    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR_OPERATIVO')")
    public ResponseEntity<AlertDTO> resolveAlert(@PathVariable String id) {
        log.info("🔔 PATCH /alerts/{}/resolve - Resolviendo alerta", id);

        try {
            Long longId = Long.parseLong(id);
            Optional<AlertEntity> alertOpt = alertRepository.findById(longId);
            if (alertOpt.isEmpty()) {
                log.warn("⚠️ Alerta no encontrada: {}", id);
                return ResponseEntity.notFound().build();
            }

            AlertEntity alert = alertOpt.get();
            alert.setEstado("RESOLVED");
            alert.setColorEstado("VERDE");
            alertRepository.save(alert);

            log.info("✅ Alerta {} resuelta", id);
            return ResponseEntity.ok(AlertDTO.fromEntity(alert));
        } catch (NumberFormatException e) {
            log.warn("⚠️ ID no es un número: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Eliminar una alerta
     * DELETE /api/v1/alerts/{id}
     *
     * Soporta búsqueda por:
     * 1. UUID (alertas de cambio de aceite o documentos)
     * 2. ID Long (compatibilidad)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR_OPERATIVO')")
    public ResponseEntity<Void> deleteAlert(@PathVariable String id) {
        log.info("🔔 DELETE /alerts/{} - Eliminando alerta", id);

        // Primero intentar buscar por UUID (válido para ambas tablas)
        Optional<AlertEntity> alertByUuid = alertRepository.findByUuid(id);
        if (alertByUuid.isPresent()) {
            alertRepository.deleteById(alertByUuid.get().getId());
            log.info("✅ Alerta {} eliminada (AlertEntity por UUID)", id);
            return ResponseEntity.noContent().build();
        }

        Optional<OilChangeAlertEntity> oilAlertByUuid = oilChangeAlertRepository.findByUuid(id);
        if (oilAlertByUuid.isPresent()) {
            oilChangeAlertRepository.deleteById(oilAlertByUuid.get().getId());
            log.info("✅ Alerta {} eliminada (OilChangeAlertEntity por UUID)", id);
            return ResponseEntity.noContent().build();
        }

        // Si no es UUID, intentar como Long ID (compatibilidad hacia atrás)
        try {
            Long longId = Long.parseLong(id);
            Optional<AlertEntity> alertOpt = alertRepository.findById(longId);
            if (alertOpt.isPresent()) {
                alertRepository.deleteById(longId);
                log.info("✅ Alerta {} eliminada (AlertEntity por ID Long)", id);
                return ResponseEntity.noContent().build();
            }

            Optional<OilChangeAlertEntity> oilAlertOpt = oilChangeAlertRepository.findById(longId);
            if (oilAlertOpt.isPresent()) {
                oilChangeAlertRepository.deleteById(longId);
                log.info("✅ Alerta {} eliminada (OilChangeAlertEntity por ID Long)", id);
                return ResponseEntity.noContent().build();
            }

            log.warn("⚠️ Alerta no encontrada: {}", id);
            return ResponseEntity.notFound().build();
        } catch (NumberFormatException e) {
            log.warn("⚠️ ID inválido (no es número ni UUID válido): {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Obtener resumen/estadísticas de alertas
     * GET /api/v1/alerts/summary
     */
    @GetMapping("/stats/summary")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR_OPERATIVO')")
    public ResponseEntity<Map<String, Object>> getAlertsSummary() {
        log.info("🔔 GET /alerts/stats/summary - Obteniendo resumen de alertas");

        List<AlertEntity> allAlerts = alertRepository.findAll();
        List<AlertEntity> activeAlerts = alertRepository.findByEstado("ACTIVE");

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalAlertas", allAlerts.size());
        summary.put("alertasActivas", activeAlerts.size());
        summary.put("alertasResueltas", allAlerts.size() - activeAlerts.size());

        // Contar por tipo
        Map<String, Long> porTipo = activeAlerts.stream()
                .collect(Collectors.groupingBy(
                        AlertEntity::getTipoAlerta,
                        Collectors.counting()
                ));
        summary.put("porTipo", porTipo);

        // Contar por color
        Map<String, Long> porColor = activeAlerts.stream()
                .collect(Collectors.groupingBy(
                        AlertEntity::getColorEstado,
                        Collectors.counting()
                ));
        summary.put("porColor", porColor);

        // Alertas vencidas (ROJO)
        long vencidas = activeAlerts.stream()
                .filter(a -> "ROJO".equals(a.getColorEstado()))
                .count();
        summary.put("alertasVencidas", vencidas);

        // Alertas próximas a vencer (AMARILLO)
        long proximasVencer = activeAlerts.stream()
                .filter(a -> "AMARILLO".equals(a.getColorEstado()))
                .count();
        summary.put("alertasProximasVencer", proximasVencer);

        return ResponseEntity.ok(summary);
    }

    /**
     * Obtener alertas vencidas (ROJO)
     * GET /api/v1/alerts/criticas
     */
    @GetMapping("/criticas")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR_OPERATIVO')")
    public ResponseEntity<List<AlertDTO>> getCriticalAlerts() {
        log.info("🔔 GET /alerts/criticas - Obteniendo alertas críticas (ROJO)");

        List<AlertDTO> alertas = alertRepository.findAlertasVencidas().stream()
                .map(AlertDTO::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(alertas);
    }

    /**
     * Obtener alertas próximas a vencer (AMARILLO)
     * GET /api/v1/alerts/warnings
     */
    @GetMapping("/warnings")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR_OPERATIVO')")
    public ResponseEntity<List<AlertDTO>> getWarningAlerts() {
        log.info("🔔 GET /alerts/warnings - Obteniendo alertas de advertencia (AMARILLO)");

        LocalDate threshold = LocalDate.now().plusDays(30);
        List<AlertDTO> alertas = alertRepository.findAlertasProximasAVencer(threshold).stream()
                .map(AlertDTO::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(alertas);
    }

    /**
     * Recalcular todas las alertas preventivas
     * POST /api/v1/alerts/recalculate
     */
    @PostMapping("/recalculate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR_OPERATIVO')")
    public ResponseEntity<Map<String, Object>> recalculateAlerts() {
        log.info("🔔 POST /alerts/recalculate - Recalculando todas las alertas");
        try {
            alertSchedulerService.calculatePreventiveAlerts();
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Alertas recalculadas exitosamente");
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error al recalcular alertas: ", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error al recalcular alertas");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}

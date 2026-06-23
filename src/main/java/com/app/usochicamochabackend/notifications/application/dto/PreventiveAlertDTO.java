package com.app.usochicamochabackend.notifications.application.dto;

import com.app.usochicamochabackend.notifications.infrastructure.entity.PreventiveAlertEntity;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PreventiveAlertDTO(
    Long id,
    @JsonProperty("assetId")
    String assetId,
    @JsonProperty("placa")
    String placa,
    @JsonProperty("assetType")
    String assetType,
    @JsonProperty("tipoMaquinaria")
    String tipoMaquinaria,
    @JsonProperty("alertType")
    String alertType,
    @JsonProperty("tipoAlerta")
    String tipoAlerta,
    String alertSubtype,
    String colorEstado,
    String estado,
    String descripcion,
    LocalDate fechaVencimiento,
    String metricType,
    Double metricValue,
    Double percentageUsed,
    LocalDateTime fechaCreacion,
    String type,
    String category,
    String icon,
    String priority,
    Long daysRemaining
) {
    public static PreventiveAlertDTO fromEntity(PreventiveAlertEntity entity) {
        String type = determineType(entity.getAlertType());
        String category = determineCategory(entity.getAlertSubtype(), entity.getAlertType());
        String icon = determineIcon(entity.getColorEstado());
        String priority = determinePriority(entity.getColorEstado());
        Long daysRemaining = calculateDaysRemaining(entity.getFechaVencimiento());

        // Nombres de campos que el frontend espera
        String tipoAlerta = buildTipoAlerta(entity.getAlertType(), entity.getAlertSubtype());
        String placa = entity.getAssetId();
        String tipoMaquinaria = entity.getAssetType();

        return new PreventiveAlertDTO(
            entity.getId(),
            entity.getAssetId(),           // assetId
            placa,                          // placa (copia de assetId)
            entity.getAssetType(),          // assetType
            tipoMaquinaria,                 // tipoMaquinaria (copia de assetType)
            entity.getAlertType(),          // alertType
            tipoAlerta,                     // tipoAlerta (construcción especial)
            entity.getAlertSubtype(),
            entity.getColorEstado(),
            entity.getEstado(),
            entity.getDescripcion(),
            entity.getFechaVencimiento(),
            entity.getMetricType(),
            entity.getMetricValue(),
            entity.getPercentageUsed(),
            entity.getFechaCreacion(),
            type,
            category,
            icon,
            priority,
            daysRemaining
        );
    }

    private static String buildTipoAlerta(String alertType, String alertSubtype) {
        if ("DOCUMENTO".equals(alertType)) {
            return "DOCUMENTO_" + (alertSubtype != null ? alertSubtype : "GENERAL");
        }
        if ("OIL_CHANGE_VEHICLE".equals(alertType)) {
            return "CAMBIO_ACEITE_VEHICULO";
        }
        if ("OIL_CHANGE_MACHINE".equals(alertType)) {
            return "CAMBIO_ACEITE_MAQUINARIA";
        }
        return alertType;
    }

    private static String determineType(String alertType) {
        if (alertType == null) return "NOTIFICATION";
        return switch (alertType) {
            case "DOCUMENTO" -> "DOCUMENT";
            case "OIL_CHANGE_VEHICLE" -> "OIL_CHANGE";
            case "OIL_CHANGE_MACHINE" -> "OIL_CHANGE";
            default -> "ALERT";
        };
    }

    private static String determineCategory(String alertSubtype, String alertType) {
        if (alertSubtype != null && !alertSubtype.isEmpty()) {
            return alertSubtype;
        }
        if (alertType != null) {
            return switch (alertType) {
                case "DOCUMENTO" -> "DOCUMENTO";
                case "OIL_CHANGE_VEHICLE" -> "VEHICLE_OIL";
                case "OIL_CHANGE_MACHINE" -> "MACHINE_OIL";
                default -> "GENERAL";
            };
        }
        return "GENERAL";
    }

    private static String determineIcon(String colorEstado) {
        if (colorEstado == null) return "bell";
        return switch (colorEstado) {
            case "ROJO" -> "alert-circle";
            case "AMARILLO" -> "alert-triangle";
            case "VERDE" -> "check-circle";
            default -> "bell";
        };
    }

    private static String determinePriority(String colorEstado) {
        if (colorEstado == null) return "LOW";
        return switch (colorEstado) {
            case "ROJO" -> "HIGH";
            case "AMARILLO" -> "MEDIUM";
            case "VERDE" -> "LOW";
            default -> "LOW";
        };
    }

    private static Long calculateDaysRemaining(LocalDate fechaVencimiento) {
        if (fechaVencimiento == null) return null;
        return java.time.temporal.ChronoUnit.DAYS.between(
            java.time.LocalDate.now(),
            fechaVencimiento
        );
    }
}

package com.app.usochicamochabackend.notifications.application.dto;

import com.app.usochicamochabackend.notifications.infrastructure.entity.AlertEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertDTO {

    private Long id;
    private String placa;
    private String tipoAlerta;
    private String descripcion;
    private LocalDate fechaVencimiento;
    private LocalDateTime fechaCreacion;
    private String estado;
    private String colorEstado;
    private Long createdByUserId;
    private Long documentoId;

    /**
     * Convierte una AlertEntity a AlertDTO
     */
    public static AlertDTO fromEntity(AlertEntity entity) {
        return AlertDTO.builder()
            .id(entity.getId())
            .placa(entity.getPlaca())
            .tipoAlerta(entity.getTipoAlerta())
            .descripcion(entity.getDescripcion())
            .fechaVencimiento(entity.getFechaVencimiento())
            .fechaCreacion(entity.getFechaCreacion())
            .estado(entity.getEstado())
            .colorEstado(entity.getColorEstado())
            .createdByUserId(entity.getCreatedByUser() != null ? entity.getCreatedByUser().getId() : null)
            .documentoId(entity.getDocumentoId())
            .build();
    }

    /**
     * Convierte AlertDTO a AlertEntity
     */
    public static AlertEntity toEntity(AlertDTO dto) {
        return AlertEntity.builder()
            .id(dto.getId())
            .placa(dto.getPlaca())
            .tipoAlerta(dto.getTipoAlerta())
            .descripcion(dto.getDescripcion())
            .fechaVencimiento(dto.getFechaVencimiento())
            .fechaCreacion(dto.getFechaCreacion())
            .estado(dto.getEstado())
            .colorEstado(dto.getColorEstado())
            .documentoId(dto.getDocumentoId())
            .build();
    }
}

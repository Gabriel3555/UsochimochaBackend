package com.app.usochicamochabackend.shared.event;

import org.springframework.context.ApplicationEvent;
import java.time.LocalDateTime;

/**
 * Evento disparado cuando se completa una inspección.
 *
 * Esto indica que el km/horas de un vehículo/moto/máquina ha sido actualizado,
 * y por lo tanto es necesario recalcular el estado de sus aceites.
 */
public class InspectionCompletedEvent extends ApplicationEvent {

    private final Long machineId;           // ID del vehículo/moto/máquina
    private final String placa;             // Placa para identificación
    private final String tipoMaquinaria;    // "VEHICULO", "MOTOCICLETA", "MAQUINARIA"
    private final Long newDistance;         // Nuevo km o horas
    private final LocalDateTime inspectionDate;

    public InspectionCompletedEvent(
        Object source,
        Long machineId,
        String placa,
        String tipoMaquinaria,
        Long newDistance,
        LocalDateTime inspectionDate
    ) {
        super(source);
        this.machineId = machineId;
        this.placa = placa;
        this.tipoMaquinaria = tipoMaquinaria;
        this.newDistance = newDistance;
        this.inspectionDate = inspectionDate;
    }

    public Long getMachineId() {
        return machineId;
    }

    public String getPlaca() {
        return placa;
    }

    public String getTipoMaquinaria() {
        return tipoMaquinaria;
    }

    public Long getNewDistance() {
        return newDistance;
    }

    public LocalDateTime getInspectionDate() {
        return inspectionDate;
    }
}

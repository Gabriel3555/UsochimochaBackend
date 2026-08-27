package com.app.usochicamochabackend.update.application.port;

import com.app.usochicamochabackend.update.application.dto.MachineOilChangeHistoryDTO;
import com.app.usochicamochabackend.update.application.dto.PerformChangeMotorOilRequest;

import java.util.List;

/**
 * Historial editable de cambios de aceite de maquinaria ("en caso de error") —
 * separado de PerformMotorOilChangeUseCase/PerformHydraulicChangeUseCase (que solo
 * cubren el alta) para poder inyectar el port en el controller sin ambigüedad de
 * beans con OilChangeService (que implementa varios ports a la vez).
 */
public interface ManageMachineOilChangeHistoryUseCase {
    List<MachineOilChangeHistoryDTO> obtenerHistorial(Long machineId, String tipo);

    void actualizarCambioAceite(Long id, PerformChangeMotorOilRequest request);

    void eliminarCambioAceite(Long id);
}

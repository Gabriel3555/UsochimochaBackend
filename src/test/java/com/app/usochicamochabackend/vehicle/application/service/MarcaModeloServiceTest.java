package com.app.usochicamochabackend.vehicle.application.service;

import com.app.usochicamochabackend.actions.application.port.SaveActionUseCase;
import com.app.usochicamochabackend.vehicle.application.dto.MarcaModeloRequest;
import com.app.usochicamochabackend.vehicle.application.dto.MarcaModeloResponse;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.MarcaModeloEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.MarcaModeloRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarcaModeloServiceTest {

    @Mock
    private MarcaModeloRepository repository;

    @Mock
    private SaveActionUseCase saveActionUseCase;

    @InjectMocks
    private MarcaModeloService marcaModeloService;

    // Los 3 métodos de escritura deben quedar auditados — antes de este fix
    // ninguno llamaba a SaveActionUseCase, a diferencia de VehicleService/
    // MachineService, que sí registran cada creación/edición/borrado.

    @Test
    void create_RegistraAccionDeAuditoria() {
        when(repository.existsByDescripcionIgnoreCase(anyString())).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> {
            MarcaModeloEntity e = inv.getArgument(0);
            e.setIdMarca(1);
            return e;
        });

        MarcaModeloResponse response = marcaModeloService.create(new MarcaModeloRequest("Toyota"));

        assertEquals("Toyota", response.descripcion());
        verify(saveActionUseCase).save(anyString());
    }

    @Test
    void update_RegistraAccionDeAuditoria() {
        MarcaModeloEntity existente = MarcaModeloEntity.builder().idMarca(1).descripcion("Toyota").build();
        when(repository.findById(1)).thenReturn(Optional.of(existente));
        when(repository.existsByDescripcionIgnoreCaseAndIdMarcaNot(anyString(), any())).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        marcaModeloService.update(1, new MarcaModeloRequest("Toyota Renombrada"));

        verify(saveActionUseCase).save(anyString());
    }

    @Test
    void delete_RegistraAccionDeAuditoria() {
        when(repository.findById(1)).thenReturn(Optional.of(
                MarcaModeloEntity.builder().idMarca(1).descripcion("Toyota").build()));

        marcaModeloService.delete(1);

        verify(saveActionUseCase).save(anyString());
    }
}

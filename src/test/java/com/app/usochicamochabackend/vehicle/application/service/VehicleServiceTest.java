package com.app.usochicamochabackend.vehicle.application.service;

import com.app.usochicamochabackend.catalog.infrastructure.repository.UbicacionRepository;
import com.app.usochicamochabackend.vehicle.application.dto.VehicleRequest;
import com.app.usochicamochabackend.vehicle.application.dto.VehicleResponse;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.VehicleEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private UbicacionRepository ubicacionRepository;

    @Mock
    private com.app.usochicamochabackend.actions.application.port.SaveActionUseCase saveActionUseCase;

    @InjectMocks
    private VehicleService vehicleService;

    private VehicleRequest requestValido;

    @BeforeEach
    void setUp() {
        requestValido = new VehicleRequest("ABC123", 1, 2, 50000, "Distrito", null, true, null, null, null);
    }

    // --- findAllVehicles ---

    @Test
    void findAllVehicles_DebeRetornarListaDeVehiculos() {
        VehicleEntity entity = new VehicleEntity();
        entity.setIdVehiculo(1);
        entity.setPlaca("ABC123");
        entity.setIdMarca(1);
        entity.setIdTipoVehiculo(2);
        entity.setKilometrajeActual(50000);
        entity.setBelongsTo("Distrito");
        entity.setActivo(true);

        when(vehicleRepository.findAllActiveVehiclesWithDocuments()).thenReturn(List.of(entity));

        List<VehicleResponse> result = vehicleService.findAllVehicles();

        assertEquals(1, result.size());
        assertEquals("ABC123", result.get(0).placa());
    }

    // --- findByPlaca ---

    @Test
    void findByPlaca_CuandoExiste_DebeRetornarVehiculo() {
        VehicleEntity entity = new VehicleEntity();
        entity.setIdVehiculo(1);
        entity.setPlaca("ABC123");
        entity.setIdMarca(1);
        entity.setIdTipoVehiculo(2);
        entity.setKilometrajeActual(50000);
        entity.setBelongsTo("Distrito");
        entity.setActivo(true);

        when(vehicleRepository.findActiveByPlaca("ABC123")).thenReturn(Optional.of(entity));

        VehicleResponse result = vehicleService.findByPlaca("ABC123");

        assertEquals("ABC123", result.placa());
    }

    @Test
    void findByPlaca_CuandoNoExiste_DebeLanzarNotFound() {
        when(vehicleRepository.findActiveByPlaca("NOEXISTE")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> vehicleService.findByPlaca("NOEXISTE"));

        assertEquals(404, ex.getStatusCode().value());
    }

    // --- createVehicle: placa única ---

    @Test
    void createVehicle_CuandoPlacaDuplicada_DebeLanzarBadRequest() {
        VehicleEntity existing = new VehicleEntity();
        existing.setActivo(true);
        when(vehicleRepository.findByPlaca("ABC123")).thenReturn(Optional.of(existing));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> vehicleService.createVehicle(requestValido));

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("placa"));
    }

    @Test
    void createVehicle_CuandoUbicacionInvalida_DebeLanzarBadRequest() {
        when(vehicleRepository.findByPlaca("ABC123")).thenReturn(Optional.empty());
        VehicleRequest reqConUbicacion = new VehicleRequest("ABC123", 1, 2, 50000, "Distrito", 99, true, null, null, null);

        when(ubicacionRepository.existsById(99)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> vehicleService.createVehicle(reqConUbicacion));

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void createVehicle_ConDatosValidos_DebeGuardarVehiculo() {
        VehicleEntity entity = new VehicleEntity();
        entity.setIdVehiculo(5);
        entity.setPlaca("ABC123");
        entity.setIdMarca(1);
        entity.setIdTipoVehiculo(2);
        entity.setKilometrajeActual(50000);
        entity.setBelongsTo("Distrito");
        entity.setActivo(true);

        when(vehicleRepository.findByPlaca("ABC123")).thenReturn(Optional.empty());
        when(vehicleRepository.save(any())).thenAnswer(inv -> {
            VehicleEntity e = inv.getArgument(0);
            e.setIdVehiculo(5);
            return e;
        });
        when(vehicleRepository.findActiveByPlaca("ABC123")).thenReturn(Optional.of(entity));

        VehicleResponse result = vehicleService.createVehicle(requestValido);

        assertEquals("ABC123", result.placa());
        assertEquals(50000, result.kilometrajeActual());
        verify(vehicleRepository).save(any(VehicleEntity.class));
    }

    // --- updateVehicle ---

    @Test
    void updateVehicle_CuandoVehiculoNoExiste_DebeLanzarNotFound() {
        when(vehicleRepository.findActiveById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> vehicleService.updateVehicle(99, requestValido));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void updateVehicle_CuandoPlacaYaPertenece_AOtroVehiculo_DebeLanzarBadRequest() {
        VehicleEntity existente = new VehicleEntity();
        existente.setIdVehiculo(1);
        existente.setPlaca("XYZ999");

        VehicleEntity otro = new VehicleEntity();
        otro.setIdVehiculo(2);
        otro.setPlaca("ABC123");

        when(vehicleRepository.findActiveById(1)).thenReturn(Optional.of(existente));
        when(vehicleRepository.findByPlaca("ABC123")).thenReturn(Optional.of(otro));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> vehicleService.updateVehicle(1, requestValido));

        assertEquals(400, ex.getStatusCode().value());
    }

    // --- deleteVehicle ---

    @Test
    void deleteVehicle_CuandoNoExiste_DebeLanzarNotFound() {
        when(vehicleRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> vehicleService.deleteVehicle(99));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void deleteVehicle_CuandoExiste_DebeDesactivarVehiculo() {
        VehicleEntity entity = new VehicleEntity();
        entity.setIdVehiculo(1);
        entity.setActivo(true);

        when(vehicleRepository.findById(1)).thenReturn(Optional.of(entity));

        vehicleService.deleteVehicle(1);

        assertFalse(entity.getActivo());
        verify(vehicleRepository).save(entity);
    }
}

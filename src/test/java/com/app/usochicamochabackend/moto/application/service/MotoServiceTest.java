package com.app.usochicamochabackend.moto.application.service;

import com.app.usochicamochabackend.actions.application.port.SaveActionUseCase;
import com.app.usochicamochabackend.catalog.infrastructure.entity.TipoVehiculoEntity;
import com.app.usochicamochabackend.catalog.infrastructure.entity.UbicacionEntity;
import com.app.usochicamochabackend.catalog.infrastructure.repository.TipoVehiculoRepository;
import com.app.usochicamochabackend.catalog.infrastructure.repository.UbicacionRepository;
import com.app.usochicamochabackend.exception.ResourceNotFoundException;
import com.app.usochicamochabackend.notifications.application.NotificationService;
import com.app.usochicamochabackend.notifications.application.PreventiveAlertCalculationService;
import com.app.usochicamochabackend.vehicle.application.dto.VehicleRequest;
import com.app.usochicamochabackend.moto.application.dto.DocumentoExistenteResponse;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleProjection;
import com.app.usochicamochabackend.moto.application.dto.MotoPlacaResponse;
import com.app.usochicamochabackend.moto.application.dto.UbicacionResponse;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.VehicleEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleRepository;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.repository.DocumentacionYElementosRepository;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.repository.InspDetalleDocumentosRepository;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.repository.InspDetalleMecanicoRepository;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.repository.InspPreOperativaRepository;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.entity.DocumentacionYElementosEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MotoServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private UbicacionRepository ubicacionRepository;
    @Mock
    private TipoVehiculoRepository tipoVehiculoRepository;
    @Mock
    private InspPreOperativaRepository inspeccionRepository;
    @Mock
    private InspDetalleDocumentosRepository detalleDocumentosRepository;
    @Mock
    private InspDetalleMecanicoRepository detalleMecanicoRepository;
    @Mock
    private DocumentacionYElementosRepository documentacionRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private SaveActionUseCase saveActionUseCase;
    @Mock
    private PreventiveAlertCalculationService preventiveAlertCalculationService;

    @InjectMocks
    private MotoService motoService;

    private VehicleEntity mockVehiculo;

    @BeforeEach
    void setUp() {
        mockVehiculo = new VehicleEntity();
        mockVehiculo.setIdVehiculo(1);
        mockVehiculo.setPlaca("MOTO123");
        mockVehiculo.setKilometrajeActual(1000);
        mockVehiculo.setActivo(true);
    }

    @Test
    void getMotocicletas_ShouldReturnList() {
        VehicleProjection proj = mock(VehicleProjection.class);
        when(proj.getId()).thenReturn(1);
        when(proj.getPlaca()).thenReturn("MOTO123");
        when(proj.getIdUbicacionBase()).thenReturn(null);
        when(proj.getUbicacionBase()).thenReturn(null);

        when(vehicleRepository.findAllActiveVehiclesByTipoName("MOTOCICLETA"))
                .thenReturn(List.of(proj));

        List<MotoPlacaResponse> result = motoService.getMotocicletas();

        assertEquals(1, result.size());
        assertEquals("MOTO123", result.get(0).placa());
    }

    @Test
    void getUbicaciones_ShouldReturnList() {
        UbicacionEntity ubicacion = new UbicacionEntity();
        ubicacion.setId(1);
        ubicacion.setNombreUbicacion("Patio Central");
        ubicacion.setActivo(true);

        when(ubicacionRepository.findByActivoTrue()).thenReturn(List.of(ubicacion));

        List<UbicacionResponse> result = motoService.getUbicaciones();

        assertEquals(1, result.size());
        assertEquals("Patio Central", result.get(0).nombreUbicacion());
    }

    @Test
    void getDocumentosByPlaca_ShouldCalculateCorrectStatus() {
        when(vehicleRepository.findByPlacaAndActivoTrue("MOTO123"))
                .thenReturn(Optional.of(mockVehiculo));

        DocumentacionYElementosEntity docSoat = new DocumentacionYElementosEntity();
        docSoat.setIdDocumento(1);
        docSoat.setFechaVencimiento(LocalDate.now().plusMonths(5));
        // URL absoluta: evita ServletUriComponentsBuilder sin contexto web en test unitario
        docSoat.setImagenUrl("https://example.com/docs/soat.jpg");

        when(documentacionRepository.findLatestByVehiculoAndTipo(anyInt(), eq("SOAT")))
                .thenReturn(Optional.of(docSoat));
        when(documentacionRepository.findLatestByVehiculoAndTipo(anyInt(), eq("TECNOMECANICA")))
                .thenReturn(Optional.empty());
        when(documentacionRepository.findLatestByVehiculoAndTipo(anyInt(), eq("LICENCIA DE CONDUCCION")))
                .thenReturn(Optional.empty());

        List<DocumentoExistenteResponse> result = motoService.getDocumentosByPlaca("MOTO123");

        assertEquals(3, result.size());

        DocumentoExistenteResponse soat = result.stream()
                .filter(d -> d.tipoDocumento().equals("SOAT")).findFirst().orElseThrow();
        assertEquals("Vigente", soat.estadoCheck());
        assertTrue(soat.imagenUrl().contains("soat.jpg"));

        DocumentoExistenteResponse tecno = result.stream()
                .filter(d -> d.tipoDocumento().equals("REVISION_TECNO")).findFirst().orElseThrow();
        assertEquals("Sin Información", tecno.estadoCheck());
    }

    @Test
    void updateMoto_RegistraAccionDeAuditoria() {
        // updateMoto era la única escritura de MotoService que no llamaba a
        // SaveActionUseCase — createMoto/deleteMoto/restoreMoto sí lo hacen.
        TipoVehiculoEntity tipoMoto = TipoVehiculoEntity.builder().id(2).nombreTipo("MOTOCICLETA").activo(true).build();
        mockVehiculo.setTipoVehiculo(tipoMoto);

        when(vehicleRepository.findById(1)).thenReturn(Optional.of(mockVehiculo));
        when(tipoVehiculoRepository.findByNombreTipoIgnoreCase(anyString())).thenReturn(Optional.of(tipoMoto));
        when(vehicleRepository.save(any())).thenReturn(mockVehiculo);

        VehicleRequest request = new VehicleRequest("MOTO123", 1, 2, 1500, "Distrito", null, true);

        motoService.updateMoto(1, request);

        verify(saveActionUseCase).save(anyString());
    }

    @Test
    void getDocumentosByPlaca_NotFound_ShouldThrowException() {
        when(vehicleRepository.findByPlacaAndActivoTrue("UNKNOWN"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                motoService.getDocumentosByPlaca("UNKNOWN"));
    }
}

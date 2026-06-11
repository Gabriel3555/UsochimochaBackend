package com.app.usochicamochabackend.notifications.application;

import com.app.usochicamochabackend.notifications.application.dto.AlertDTO;
import com.app.usochicamochabackend.notifications.infrastructure.entity.AlertEntity;
import com.app.usochicamochabackend.notifications.infrastructure.repository.AlertRepository;
import com.app.usochicamochabackend.update.infrastructure.entity.VehicleOilChangeEntity;
import com.app.usochicamochabackend.update.infrastructure.repository.VehicleOilChangeRepository;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.VehicleEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleRepository;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.entity.DocumentacionYElementosEntity;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.repository.DocumentacionYElementosRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertCalculationServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private VehicleOilChangeRepository vehicleOilChangeRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private DocumentacionYElementosRepository documentacionRepository;

    @InjectMocks
    private AlertCalculationService alertCalculationService;

    private String testPlaca;
    private VehicleEntity testVehicle;
    private VehicleOilChangeEntity lastVehicleOilChange;
    private DocumentacionYElementosEntity expiringDocument;

    @BeforeEach
    void setUp() {
        testPlaca = "ABC123";

        // Setup: vehículo de prueba
        testVehicle = VehicleEntity.builder()
            .idVehiculo(1)
            .placa(testPlaca)
            .build();

        // Setup: último cambio de aceite hace 7 meses
        lastVehicleOilChange = VehicleOilChangeEntity.builder()
            .dateStamp(LocalDateTime.now().minusMonths(7))
            .oilType("Mineral 15W-40")
            .quantity(5.0)
            .build();

        // Setup: documento que vence en 15 días
        expiringDocument = DocumentacionYElementosEntity.builder()
            .idDocumento(1)
            .idVehiculo(1)
            .tipoDocumento("SOAT")
            .fechaVencimiento(LocalDate.now().plusDays(15))
            .activo(true)
            .build();
    }

    // ============ TESTS: Cambio de Aceite Vencido ============

    @Test
    void calculateAlertsForPlate_CambioAceiteVencido_DebeCrearAlerta() {
        // DADO: vehículo sin alerta activa y con cambio de aceite hace 7 meses
        when(vehicleOilChangeRepository.findAllByPlacaOrderByDateStampDesc(testPlaca))
                .thenReturn(List.of(lastVehicleOilChange));
        when(vehicleRepository.findByPlaca(testPlaca))
                .thenReturn(Optional.of(testVehicle));
        when(alertRepository.findTopByPlacaAndTipoAlertaAndEstadoOrderByFechaCreacionDesc(
                testPlaca, "CAMBIO_ACEITE", "ACTIVE"))
                .thenReturn(Optional.empty());
        when(documentacionRepository.findAll())
                .thenReturn(List.of());

        // CUANDO: se calculan alertas para el vehículo
        alertCalculationService.calculateAlertsForPlate(testPlaca, "VEHÍCULO");

        // ENTONCES: debe crear una alerta de cambio de aceite
        ArgumentCaptor<AlertEntity> captor = ArgumentCaptor.forClass(AlertEntity.class);
        verify(alertRepository).save(captor.capture());

        AlertEntity alertCreated = captor.getValue();
        assertEquals(testPlaca, alertCreated.getPlaca());
        assertEquals("CAMBIO_ACEITE", alertCreated.getTipoAlerta());
        assertEquals("ACTIVE", alertCreated.getEstado());
        assertTrue(alertCreated.getDescripcion().contains("Cambio de aceite"));
    }

    @Test
    void calculateAlertsForPlate_CambioAceiteReciente_NoDebeCrearAlerta() {
        // DADO: cambio de aceite hace solo 2 meses
        VehicleOilChangeEntity recentOilChange = VehicleOilChangeEntity.builder()
            .dateStamp(LocalDateTime.now().minusMonths(2))
            .oilType("Mineral 15W-40")
            .quantity(5.0)
            .build();

        when(vehicleOilChangeRepository.findAllByPlacaOrderByDateStampDesc(testPlaca))
                .thenReturn(List.of(recentOilChange));
        when(vehicleRepository.findByPlaca(testPlaca))
                .thenReturn(Optional.of(testVehicle));
        when(documentacionRepository.findAll())
                .thenReturn(List.of());

        // CUANDO: se calculan alertas
        alertCalculationService.calculateAlertsForPlate(testPlaca, "VEHÍCULO");

        // ENTONCES: NO debe crear alerta
        verify(alertRepository, never()).save(any(AlertEntity.class));
    }

    @Test
    void calculateAlertsForPlate_VehiculoSinCambioAceite_NoDebeCrearAlerta() {
        // DADO: vehículo sin registro de cambio de aceite
        when(vehicleOilChangeRepository.findAllByPlacaOrderByDateStampDesc(testPlaca))
                .thenReturn(List.of());
        when(vehicleRepository.findByPlaca(testPlaca))
                .thenReturn(Optional.of(testVehicle));
        when(documentacionRepository.findAll())
                .thenReturn(List.of());

        // CUANDO: se calculan alertas
        alertCalculationService.calculateAlertsForPlate(testPlaca, "VEHÍCULO");

        // ENTONCES: NO debe crear alerta
        verify(alertRepository, never()).save(any(AlertEntity.class));
    }

    // ============ TESTS: Documentos Próximos a Vencer ============

    @Test
    void calculateAlertsForPlate_DocumentoProximoAVencer_DebeCrearAlerta() {
        // DADO: documento que vence en 15 días (dentro del rango 0-30)
        when(vehicleOilChangeRepository.findAllByPlacaOrderByDateStampDesc(testPlaca))
                .thenReturn(List.of());
        when(vehicleRepository.findByPlaca(testPlaca))
                .thenReturn(Optional.of(testVehicle));
        when(documentacionRepository.findAll())
                .thenReturn(List.of(expiringDocument));
        when(alertRepository.findTopByPlacaAndTipoAlertaAndEstadoOrderByFechaCreacionDesc(
                testPlaca, "DOCUMENTO", "ACTIVE"))
                .thenReturn(Optional.empty());

        // CUANDO: se calculan alertas
        alertCalculationService.calculateAlertsForPlate(testPlaca, "VEHÍCULO");

        // ENTONCES: debe crear 1 alerta de documento
        ArgumentCaptor<AlertEntity> captor = ArgumentCaptor.forClass(AlertEntity.class);
        verify(alertRepository).save(captor.capture());

        AlertEntity alertCreated = captor.getValue();
        assertEquals(testPlaca, alertCreated.getPlaca());
        assertEquals("DOCUMENTO", alertCreated.getTipoAlerta());
    }

    @Test
    void calculateAlertsForPlate_DocumentoVencido_NoDebeCrearAlerta() {
        // DADO: documento vencido (fecha < hoy)
        DocumentacionYElementosEntity expiredDoc = DocumentacionYElementosEntity.builder()
            .idDocumento(2)
            .idVehiculo(1)
            .tipoDocumento("RUNT")
            .fechaVencimiento(LocalDate.now().minusDays(5))
            .activo(true)
            .build();

        when(vehicleOilChangeRepository.findAllByPlacaOrderByDateStampDesc(testPlaca))
                .thenReturn(List.of());
        when(vehicleRepository.findByPlaca(testPlaca))
                .thenReturn(Optional.of(testVehicle));
        when(documentacionRepository.findAll())
                .thenReturn(List.of(expiredDoc));

        // CUANDO: se calculan alertas
        alertCalculationService.calculateAlertsForPlate(testPlaca, "VEHÍCULO");

        // ENTONCES: NO debe crear alerta (está fuera del rango)
        verify(alertRepository, never()).save(any(AlertEntity.class));
    }

    @Test
    void calculateAlertsForPlate_DocumentoLejanoAVencer_NoDebeCrearAlerta() {
        // DADO: documento que vence en 45 días (fuera del rango de 30 días)
        DocumentacionYElementosEntity distantDoc = DocumentacionYElementosEntity.builder()
            .idDocumento(3)
            .idVehiculo(1)
            .tipoDocumento("EXTINTOR")
            .fechaVencimiento(LocalDate.now().plusDays(45))
            .activo(true)
            .build();

        when(vehicleOilChangeRepository.findAllByPlacaOrderByDateStampDesc(testPlaca))
                .thenReturn(List.of());
        when(vehicleRepository.findByPlaca(testPlaca))
                .thenReturn(Optional.of(testVehicle));
        when(documentacionRepository.findAll())
                .thenReturn(List.of(distantDoc));

        // CUANDO: se calculan alertas
        alertCalculationService.calculateAlertsForPlate(testPlaca, "VEHÍCULO");

        // ENTONCES: NO debe crear alerta
        verify(alertRepository, never()).save(any(AlertEntity.class));
    }

    // ============ TESTS: Prevención de Duplicados ============

    @Test
    void calculateAlertsForPlate_AlertaActivaExistente_NoDebeDuplicar() {
        // DADO: una alerta ACTIVE ya existe para ese tipo
        AlertEntity existingAlert = AlertEntity.builder()
            .placa(testPlaca)
            .tipoAlerta("CAMBIO_ACEITE")
            .estado("ACTIVE")
            .build();

        when(vehicleOilChangeRepository.findAllByPlacaOrderByDateStampDesc(testPlaca))
                .thenReturn(List.of(lastVehicleOilChange));
        when(vehicleRepository.findByPlaca(testPlaca))
                .thenReturn(Optional.of(testVehicle));
        when(alertRepository.findTopByPlacaAndTipoAlertaAndEstadoOrderByFechaCreacionDesc(
                testPlaca, "CAMBIO_ACEITE", "ACTIVE"))
                .thenReturn(Optional.of(existingAlert));
        when(documentacionRepository.findAll())
                .thenReturn(List.of());

        // CUANDO: se calculan alertas nuevamente
        alertCalculationService.calculateAlertsForPlate(testPlaca, "VEHÍCULO");

        // ENTONCES: NO debe hacer save (porque ya existe la alerta)
        verify(alertRepository, never()).save(any(AlertEntity.class));
    }

    // ============ TESTS: Notificación WebSocket ============

    @Test
    void calculateAlertsForPlate_AlCriarAlerta_DebeNotificarPorWebSocket() {
        // DADO: condiciones para crear alerta
        when(vehicleOilChangeRepository.findAllByPlacaOrderByDateStampDesc(testPlaca))
                .thenReturn(List.of(lastVehicleOilChange));
        when(vehicleRepository.findByPlaca(testPlaca))
                .thenReturn(Optional.of(testVehicle));
        when(alertRepository.findTopByPlacaAndTipoAlertaAndEstadoOrderByFechaCreacionDesc(
                testPlaca, "CAMBIO_ACEITE", "ACTIVE"))
                .thenReturn(Optional.empty());
        when(documentacionRepository.findAll())
                .thenReturn(List.of());

        // CUANDO: se crea una alerta
        alertCalculationService.calculateAlertsForPlate(testPlaca, "VEHÍCULO");

        // ENTONCES: debe notificar por WebSocket
        verify(notificationService).notifyAlert(any(AlertDTO.class));
    }

    // ============ TESTS: Cálculo de Color Estado ============

    @Test
    void createAlert_DebeCalcularColorSegunFechaVencimiento() {
        // DADO: alerta que será creada
        when(vehicleOilChangeRepository.findAllByPlacaOrderByDateStampDesc(testPlaca))
                .thenReturn(List.of(lastVehicleOilChange));
        when(vehicleRepository.findByPlaca(testPlaca))
                .thenReturn(Optional.of(testVehicle));
        when(alertRepository.findTopByPlacaAndTipoAlertaAndEstadoOrderByFechaCreacionDesc(
                testPlaca, "CAMBIO_ACEITE", "ACTIVE"))
                .thenReturn(Optional.empty());
        when(documentacionRepository.findAll())
                .thenReturn(List.of());

        // CUANDO: se crea la alerta
        alertCalculationService.calculateAlertsForPlate(testPlaca, "VEHÍCULO");

        // ENTONCES: color debe ser ROJO (vencida) o AMARILLO (próxima a vencer)
        ArgumentCaptor<AlertEntity> captor = ArgumentCaptor.forClass(AlertEntity.class);
        verify(alertRepository).save(captor.capture());

        AlertEntity alertCreated = captor.getValue();
        assertNotNull(alertCreated.getColorEstado());
        assertTrue(alertCreated.getColorEstado().equals("ROJO") ||
                   alertCreated.getColorEstado().equals("AMARILLO"));
    }

    // ============ TESTS: Múltiples Documentos ============

    @Test
    void calculateAlertsForPlate_MultipleDocumentosProximosAVencer_DebeCrearAlertaPorCadaDocumento() {
        // DADO: 2 documentos próximos a vencer
        DocumentacionYElementosEntity doc1 = DocumentacionYElementosEntity.builder()
            .idDocumento(1)
            .idVehiculo(1)
            .tipoDocumento("SOAT")
            .fechaVencimiento(LocalDate.now().plusDays(15))
            .activo(true)
            .build();

        DocumentacionYElementosEntity doc2 = DocumentacionYElementosEntity.builder()
            .idDocumento(2)
            .idVehiculo(1)
            .tipoDocumento("RUNT")
            .fechaVencimiento(LocalDate.now().plusDays(25))
            .activo(true)
            .build();

        when(vehicleOilChangeRepository.findAllByPlacaOrderByDateStampDesc(testPlaca))
                .thenReturn(List.of());
        when(vehicleRepository.findByPlaca(testPlaca))
                .thenReturn(Optional.of(testVehicle));
        when(documentacionRepository.findAll())
                .thenReturn(List.of(doc1, doc2));
        when(alertRepository.findTopByPlacaAndTipoAlertaAndEstadoOrderByFechaCreacionDesc(
                testPlaca, "DOCUMENTO", "ACTIVE"))
                .thenReturn(Optional.empty());

        // CUANDO: se calculan alertas
        alertCalculationService.calculateAlertsForPlate(testPlaca, "VEHÍCULO");

        // ENTONCES: debe crear 2 alertas (una por documento)
        ArgumentCaptor<AlertEntity> captor = ArgumentCaptor.forClass(AlertEntity.class);
        verify(alertRepository, times(2)).save(captor.capture());

        List<AlertEntity> alerts = captor.getAllValues();
        assertEquals(2, alerts.size());
        assertTrue(alerts.stream().allMatch(a -> "DOCUMENTO".equals(a.getTipoAlerta())));
    }

    // ============ TESTS: Vehiculo no Existe ============

    @Test
    void calculateAlertsForPlate_VehiculoNoExiste_NoDebeCrearAlerta() {
        // DADO: vehículo que no existe en BD
        when(vehicleOilChangeRepository.findAllByPlacaOrderByDateStampDesc(testPlaca))
                .thenReturn(List.of());
        when(vehicleRepository.findByPlaca(testPlaca))
                .thenReturn(Optional.empty());
        when(documentacionRepository.findAll())
                .thenReturn(List.of(expiringDocument));

        // CUANDO: se calculan alertas
        alertCalculationService.calculateAlertsForPlate(testPlaca, "VEHÍCULO");

        // ENTONCES: NO debe crear alerta (vehículo no existe)
        verify(alertRepository, never()).save(any(AlertEntity.class));
    }

    // ============ TESTS: Error Handling ============

    @Test
    void calculateAlertsForPlate_ConExcepcion_DebeLoguearYContinuar() {
        // DADO: repositorio lanza excepción
        when(vehicleOilChangeRepository.findAllByPlacaOrderByDateStampDesc(testPlaca))
                .thenThrow(new RuntimeException("DB error"));
        when(documentacionRepository.findAll())
                .thenReturn(List.of());

        // CUANDO: se calculan alertas
        // ENTONCES: no debe lanzar excepción (manejo interno)
        assertDoesNotThrow(() -> alertCalculationService.calculateAlertsForPlate(testPlaca, "VEHÍCULO"));
    }
}

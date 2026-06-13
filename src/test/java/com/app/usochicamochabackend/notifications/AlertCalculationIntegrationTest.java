package com.app.usochicamochabackend.notifications;

import com.app.usochicamochabackend.notifications.application.AlertCalculationService;
import com.app.usochicamochabackend.notifications.infrastructure.entity.AlertEntity;
import com.app.usochicamochabackend.notifications.infrastructure.repository.AlertRepository;
import com.app.usochicamochabackend.update.infrastructure.entity.VehicleOilChangeEntity;
import com.app.usochicamochabackend.update.infrastructure.entity.OilType;
import com.app.usochicamochabackend.update.infrastructure.repository.VehicleOilChangeRepository;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.VehicleEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleRepository;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.entity.DocumentacionYElementosEntity;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.repository.DocumentacionYElementosRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("e2e")
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AlertCalculationIntegrationTest {

    @Autowired
    private AlertCalculationService alertCalculationService;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private VehicleOilChangeRepository vehicleOilChangeRepository;

    @Autowired
    private DocumentacionYElementosRepository documentacionRepository;

    private String testPlaca;
    private VehicleEntity testVehicle;

    @BeforeEach
    void setUp() {
        // Limpiar datos previos
        alertRepository.deleteAll();
        vehicleOilChangeRepository.deleteAll();
        documentacionRepository.deleteAll();
        vehicleRepository.deleteAll();

        testPlaca = "ABC123";

        // Crear vehículo de prueba
        testVehicle = VehicleEntity.builder()
            .placa(testPlaca)
            .idMarca(1)
            .idTipoVehiculo(2)
            .kilometrajeActual(50000)
            .belongsTo("Distrito")
            .activo(true)
            .build();
        testVehicle = vehicleRepository.save(testVehicle);
    }

    // ============ TESTS: Creación de Alertas ============

    @Test
    void integrationTest_CambioAceiteVencido_DebeCrearAlerta() {
        // DADO: cambio de aceite hace 7 meses
        VehicleOilChangeEntity oilChange = VehicleOilChangeEntity.builder()
            .vehicle(testVehicle)
            .dateStamp(LocalDateTime.now().minusMonths(7))
            .oilType(OilType.MOTOR)
            .quantity(5.0)
            .build();
        vehicleOilChangeRepository.save(oilChange);

        // CUANDO: se calcula alerta
        alertCalculationService.calculateAlertsForPlate(testPlaca, "VEHÍCULO");

        // ENTONCES: debe existir alerta en BD
        List<AlertEntity> alerts = alertRepository.findByPlacaAndEstado(testPlaca, "ACTIVE");
        assertEquals(1, alerts.size());
        assertEquals("CAMBIO_ACEITE", alerts.get(0).getTipoAlerta());
        assertEquals("ACTIVE", alerts.get(0).getEstado());
    }

    @Test
    void integrationTest_DocumentoProximoAVencer_DebeCrearAlerta() {
        // DADO: documento próximo a vencer en 15 días
        DocumentacionYElementosEntity doc = DocumentacionYElementosEntity.builder()
            .idVehiculo(testVehicle.getIdVehiculo())
            .tipoDocumento("SOAT")
            .fechaVencimiento(LocalDate.now().plusDays(15))
            .activo(true)
            .build();
        documentacionRepository.save(doc);

        // CUANDO: se calcula alerta
        alertCalculationService.calculateAlertsForPlate(testPlaca, "VEHÍCULO");

        // ENTONCES: debe existir alerta de documento
        List<AlertEntity> alerts = alertRepository.findByPlacaAndTipoAlertaOrderByFechaCreacionDesc(testPlaca, "DOCUMENTO");
        assertEquals(1, alerts.size());
        assertEquals("ACTIVE", alerts.get(0).getEstado());
    }

    @Test
    void integrationTest_MultipleAlertas_DebeCrearTodasAlMismoTiempo() {
        // DADO: cambio de aceite vencido + documento próximo a vencer
        VehicleOilChangeEntity oilChange = VehicleOilChangeEntity.builder()
            .vehicle(testVehicle)
            .dateStamp(LocalDateTime.now().minusMonths(7))
            .oilType(OilType.MOTOR)
            .quantity(5.0)
            .build();
        vehicleOilChangeRepository.save(oilChange);

        DocumentacionYElementosEntity doc = DocumentacionYElementosEntity.builder()
            .idVehiculo(testVehicle.getIdVehiculo())
            .tipoDocumento("RUNT")
            .fechaVencimiento(LocalDate.now().plusDays(20))
            .activo(true)
            .build();
        documentacionRepository.save(doc);

        // CUANDO: se calculan alertas
        alertCalculationService.calculateAlertsForPlate(testPlaca, "VEHÍCULO");

        // ENTONCES: debe haber 2 alertas activas
        List<AlertEntity> allAlerts = alertRepository.findByPlacaAndEstado(testPlaca, "ACTIVE");
        assertEquals(2, allAlerts.size());
        assertTrue(allAlerts.stream().anyMatch(a -> "CAMBIO_ACEITE".equals(a.getTipoAlerta())));
        assertTrue(allAlerts.stream().anyMatch(a -> "DOCUMENTO".equals(a.getTipoAlerta())));
    }

    // ============ TESTS: Prevención de Duplicados ============

    @Test
    void integrationTest_ExecutarCalculoDosVeces_NoDebeCrearDuplicados() {
        // DADO: cambio de aceite vencido
        VehicleOilChangeEntity oilChange = VehicleOilChangeEntity.builder()
            .vehicle(testVehicle)
            .dateStamp(LocalDateTime.now().minusMonths(7))
            .oilType(OilType.MOTOR)
            .quantity(5.0)
            .build();
        vehicleOilChangeRepository.save(oilChange);

        // CUANDO: se calcula alerta dos veces
        alertCalculationService.calculateAlertsForPlate(testPlaca, "VEHÍCULO");
        alertCalculationService.calculateAlertsForPlate(testPlaca, "VEHÍCULO");

        // ENTONCES: debe haber solo 1 alerta (no duplicada)
        List<AlertEntity> alerts = alertRepository.findByPlacaAndEstado(testPlaca, "ACTIVE");
        assertEquals(1, alerts.size());
    }

    // ============ TESTS: Cálculo de Color Estado ============

    @Test
    void integrationTest_AlertaVencida_DebeSerRoja() {
        // DADO: cambio de aceite vencido hace más de 6 meses
        VehicleOilChangeEntity oilChange = VehicleOilChangeEntity.builder()
            .vehicle(testVehicle)
            .dateStamp(LocalDateTime.now().minusMonths(8))
            .oilType(OilType.MOTOR)
            .quantity(5.0)
            .build();
        vehicleOilChangeRepository.save(oilChange);

        // CUANDO: se calcula alerta
        alertCalculationService.calculateAlertsForPlate(testPlaca, "VEHÍCULO");

        // ENTONCES: color debe ser ROJO
        List<AlertEntity> alerts = alertRepository.findByPlacaAndEstado(testPlaca, "ACTIVE");
        assertEquals(1, alerts.size());
        assertEquals("ROJO", alerts.get(0).getColorEstado());
    }

    @Test
    void integrationTest_AlertaProximaAVencer_DebeSerAmarilla() {
        // DADO: documento que vence en 10 días (dentro de <30 pero no vencido)
        DocumentacionYElementosEntity doc = DocumentacionYElementosEntity.builder()
            .idVehiculo(testVehicle.getIdVehiculo())
            .tipoDocumento("SOAT")
            .fechaVencimiento(LocalDate.now().plusDays(10))
            .activo(true)
            .build();
        documentacionRepository.save(doc);

        // CUANDO: se calcula alerta
        alertCalculationService.calculateAlertsForPlate(testPlaca, "VEHÍCULO");

        // ENTONCES: color debe ser AMARILLO
        List<AlertEntity> alerts = alertRepository.findByPlacaAndEstado(testPlaca, "ACTIVE");
        assertEquals(1, alerts.size());
        assertEquals("AMARILLO", alerts.get(0).getColorEstado());
    }

    // ============ TESTS: Query Methods ============

    @Test
    void integrationTest_FindAlertasVencidas_DebeRetornarSoloRojas() {
        // DADO: una alerta ROJA (vencida)
        VehicleOilChangeEntity oilChange = VehicleOilChangeEntity.builder()
            .vehicle(testVehicle)
            .dateStamp(LocalDateTime.now().minusMonths(9))
            .oilType(OilType.MOTOR)
            .quantity(5.0)
            .build();
        vehicleOilChangeRepository.save(oilChange);
        alertCalculationService.calculateAlertsForPlate(testPlaca, "VEHÍCULO");

        // CUANDO: se buscan alertas vencidas
        List<AlertEntity> vencidas = alertRepository.findAlertasVencidas();

        // ENTONCES: debe contener la alerta roja
        assertTrue(vencidas.stream().anyMatch(a -> a.getPlaca().equals(testPlaca)));
    }

    @Test
    void integrationTest_CountActiveAlerts_DebeContar() {
        // DADO: 2 alertas activas
        VehicleOilChangeEntity oilChange = VehicleOilChangeEntity.builder()
            .vehicle(testVehicle)
            .dateStamp(LocalDateTime.now().minusMonths(7))
            .oilType(OilType.MOTOR)
            .quantity(5.0)
            .build();
        vehicleOilChangeRepository.save(oilChange);

        DocumentacionYElementosEntity doc = DocumentacionYElementosEntity.builder()
            .idVehiculo(testVehicle.getIdVehiculo())
            .tipoDocumento("SOAT")
            .fechaVencimiento(LocalDate.now().plusDays(15))
            .activo(true)
            .build();
        documentacionRepository.save(doc);

        alertCalculationService.calculateAlertsForPlate(testPlaca, "VEHÍCULO");

        // CUANDO: se cuentan alertas activas
        long totalActive = alertRepository.countActiveAlerts();

        // ENTONCES: debe ser >= 2
        assertTrue(totalActive >= 2);
    }

    // ============ TESTS: Datos Ausentes ============

    @Test
    void integrationTest_SinDatos_NoDebeCrearAlerta() {
        // DADO: vehículo sin cambios de aceite ni documentos
        // (ya está en setUp())

        // CUANDO: se calcula alerta
        alertCalculationService.calculateAlertsForPlate(testPlaca, "VEHÍCULO");

        // ENTONCES: no debe haber alertas
        List<AlertEntity> alerts = alertRepository.findByPlacaAndEstado(testPlaca, "ACTIVE");
        assertEquals(0, alerts.size());
    }

    @Test
    void integrationTest_CambioAceiteReciente_NoDebeCrearAlerta() {
        // DADO: cambio de aceite hace solo 2 meses
        VehicleOilChangeEntity oilChange = VehicleOilChangeEntity.builder()
            .vehicle(testVehicle)
            .dateStamp(LocalDateTime.now().minusMonths(2))
            .oilType(OilType.MOTOR)
            .quantity(5.0)
            .build();
        vehicleOilChangeRepository.save(oilChange);

        // CUANDO: se calcula alerta
        alertCalculationService.calculateAlertsForPlate(testPlaca, "VEHÍCULO");

        // ENTONCES: no debe haber alerta
        List<AlertEntity> alerts = alertRepository.findByPlacaAndEstado(testPlaca, "ACTIVE");
        assertEquals(0, alerts.size());
    }

    @Test
    void integrationTest_DocumentoLejanoAVencer_NoDebeCrearAlerta() {
        // DADO: documento que vence en 60 días (fuera del rango de 30)
        DocumentacionYElementosEntity doc = DocumentacionYElementosEntity.builder()
            .idVehiculo(testVehicle.getIdVehiculo())
            .tipoDocumento("RUNT")
            .fechaVencimiento(LocalDate.now().plusDays(60))
            .activo(true)
            .build();
        documentacionRepository.save(doc);

        // CUANDO: se calcula alerta
        alertCalculationService.calculateAlertsForPlate(testPlaca, "VEHÍCULO");

        // ENTONCES: no debe haber alerta
        List<AlertEntity> alerts = alertRepository.findByPlacaAndEstado(testPlaca, "ACTIVE");
        assertEquals(0, alerts.size());
    }

    // ============ TESTS: Descripción y Metadatos ============

    @Test
    void integrationTest_AlertaDebeContenerDescripcionCorrecta() {
        // DADO: cambio de aceite hace 7 meses
        VehicleOilChangeEntity oilChange = VehicleOilChangeEntity.builder()
            .vehicle(testVehicle)
            .dateStamp(LocalDateTime.now().minusMonths(7))
            .oilType(OilType.MOTOR)
            .quantity(5.0)
            .build();
        vehicleOilChangeRepository.save(oilChange);

        // CUANDO: se crea alerta
        alertCalculationService.calculateAlertsForPlate(testPlaca, "VEHÍCULO");

        // ENTONCES: descripción debe ser informativa
        List<AlertEntity> alerts = alertRepository.findByPlacaAndEstado(testPlaca, "ACTIVE");
        assertEquals(1, alerts.size());
        assertTrue(alerts.get(0).getDescripcion().contains("Cambio de aceite"));
        assertTrue(alerts.get(0).getDescripcion().contains("días"));
    }

    @Test
    void integrationTest_AlertaDebeContenerFechaVencimiento() {
        // DADO: cambio de aceite hace 7 meses
        LocalDateTime oilChangeDate = LocalDateTime.now().minusMonths(7);
        VehicleOilChangeEntity oilChange = VehicleOilChangeEntity.builder()
            .vehicle(testVehicle)
            .dateStamp(oilChangeDate)
            .oilType(OilType.MOTOR)
            .quantity(5.0)
            .build();
        vehicleOilChangeRepository.save(oilChange);

        // CUANDO: se crea alerta
        alertCalculationService.calculateAlertsForPlate(testPlaca, "VEHÍCULO");

        // ENTONCES: fecha de vencimiento debe ser aproximadamente 6 meses después del cambio
        List<AlertEntity> alerts = alertRepository.findByPlacaAndEstado(testPlaca, "ACTIVE");
        assertEquals(1, alerts.size());
        assertNotNull(alerts.get(0).getFechaVencimiento());
        LocalDate expectedDate = oilChangeDate.toLocalDate().plusMonths(6);
        assertEquals(expectedDate, alerts.get(0).getFechaVencimiento());
    }

    @Test
    void integrationTest_AlertaDebeContenerFechaCreacion() {
        // DADO: cambio de aceite vencido
        VehicleOilChangeEntity oilChange = VehicleOilChangeEntity.builder()
            .vehicle(testVehicle)
            .dateStamp(LocalDateTime.now().minusMonths(7))
            .oilType(OilType.MOTOR)
            .quantity(5.0)
            .build();
        vehicleOilChangeRepository.save(oilChange);

        // CUANDO: se crea alerta
        alertCalculationService.calculateAlertsForPlate(testPlaca, "VEHÍCULO");

        // ENTONCES: fecha de creación debe ser HOY
        List<AlertEntity> alerts = alertRepository.findByPlacaAndEstado(testPlaca, "ACTIVE");
        assertEquals(1, alerts.size());
        assertNotNull(alerts.get(0).getFechaCreacion());
        assertEquals(LocalDate.now(), alerts.get(0).getFechaCreacion().toLocalDate());
    }
}

package com.app.usochicamochabackend.notifications;

import com.app.usochicamochabackend.catalog.infrastructure.entity.TipoVehiculoEntity;
import com.app.usochicamochabackend.catalog.infrastructure.repository.TipoVehiculoRepository;
import com.app.usochicamochabackend.machine.application.dto.MachineRequest;
import com.app.usochicamochabackend.machine.application.port.CreateMachineUseCase;
import com.app.usochicamochabackend.machine.application.port.UpdateMachineUseCase;
import com.app.usochicamochabackend.machine.application.dto.MachineResponse;
import com.app.usochicamochabackend.machine.infrastructure.repository.MachineRepository;
import com.app.usochicamochabackend.notifications.infrastructure.repository.PreventiveAlertRepository;
import com.app.usochicamochabackend.user.application.dto.CreateUserRequest;
import com.app.usochicamochabackend.user.application.dto.CreateUserResponse;
import com.app.usochicamochabackend.user.application.dto.UpdateUserRequest;
import com.app.usochicamochabackend.user.application.port.CreateUserUseCase;
import com.app.usochicamochabackend.user.application.port.UpdateUserUseCase;
import com.app.usochicamochabackend.auth.infrastructure.repository.UserRepositoryJpa;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.MarcaModeloEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.VehicleEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.MarcaModeloRepository;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleRepository;
import com.app.usochicamochabackend.vehicleinspection.application.dto.VehicleDocumentRequest;
import com.app.usochicamochabackend.vehicleinspection.application.service.VehiculoInspectionService;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.repository.DocumentacionYElementosRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E real (sin mocks) del flujo reportado por el usuario: al subir/actualizar un documento
 * (SOAT, seguro todo riesgo, licencia), la alerta preventiva correspondiente debe aparecer o
 * desaparecer de inmediato — sin llamar manualmente a /alerts/refresh y sin esperar al cron.
 * Corre contra H2 en memoria (perfil "test"), no toca la BD real de desarrollo.
 */
@Tag("e2e")
@SpringBootTest
@ActiveProfiles("test")
class PreventiveAlertsE2ETest {

    @Autowired private CreateMachineUseCase createMachineUseCase;
    @Autowired private UpdateMachineUseCase updateMachineUseCase;
    @Autowired private MachineRepository machineRepository;

    @Autowired private CreateUserUseCase createUserUseCase;
    @Autowired private UpdateUserUseCase updateUserUseCase;
    @Autowired private UserRepositoryJpa userRepository;

    @Autowired private VehiculoInspectionService vehiculoInspectionService;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private MarcaModeloRepository marcaModeloRepository;
    @Autowired private TipoVehiculoRepository tipoVehiculoRepository;
    @Autowired private DocumentacionYElementosRepository documentacionRepository;

    @Autowired private PreventiveAlertRepository alertRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        alertRepository.deleteAll();
        documentacionRepository.deleteAll();
        vehicleRepository.deleteAll();
        tipoVehiculoRepository.deleteAll();
        marcaModeloRepository.deleteAll();
        machineRepository.deleteAll();
        userRepository.deleteAll();
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    private List<com.app.usochicamochabackend.notifications.infrastructure.entity.PreventiveAlertEntity> activeAlertsFor(String assetId) {
        return alertRepository.findByAssetIdAndEstado(assetId, "ACTIVA");
    }

    /** Alertas de tipo DOCUMENTO únicamente (excluye las informativas VERDE de "primer cambio de aceite"). */
    private List<com.app.usochicamochabackend.notifications.infrastructure.entity.PreventiveAlertEntity> activeDocumentAlertsFor(String assetId) {
        return activeAlertsFor(assetId).stream()
                .filter(a -> "DOCUMENTO".equals(a.getAlertType()))
                .toList();
    }

    @Test
    void machineSoatAndInsurance_AppearAndClearIndependently() {
        // Crear máquina con SOAT vencido y seguro todo riesgo vigente
        MachineRequest req = new MachineRequest(
                "Excavadora E2E", "Distrito", "320D",
                LocalDate.now().minusDays(5), // SOAT vencido
                "Caterpillar",
                LocalDate.now().plusDays(200), // seguro todo riesgo vigente
                "ENG-E2E", "ID-E2E", BigDecimal.TEN, BigDecimal.ONE, "GAL_PER_HOUR");

        MachineResponse created = createMachineUseCase.createMachine(req);
        String assetId = created.name();

        // La alerta de SOAT debe existir de inmediato (sin llamar /alerts/refresh)
        var alertsAfterCreate = activeDocumentAlertsFor(assetId);
        assertThat(alertsAfterCreate)
                .as("Debe crearse la alerta ROJA de SOAT vencido al crear la máquina")
                .anySatisfy(a -> {
                    assertThat(a.getAlertType()).isEqualTo("DOCUMENTO");
                    assertThat(a.getAlertSubtype()).isEqualTo("SOAT");
                    assertThat(a.getColorEstado()).isEqualTo("ROJO");
                });
        // El seguro todo riesgo está vigente (200 días) → no debe generar alerta
        assertThat(alertsAfterCreate)
                .noneMatch(a -> "SEGURO TODO RIESGO".equals(a.getAlertSubtype()));

        // Renovar el SOAT a una fecha lejana → la alerta de SOAT debe desaparecer de inmediato
        MachineRequest renewSoat = new MachineRequest(
                "Excavadora E2E", "Distrito", "320D",
                LocalDate.now().plusDays(365),
                "Caterpillar",
                LocalDate.now().plusDays(200),
                "ENG-E2E", "ID-E2E", BigDecimal.TEN, BigDecimal.ONE, "GAL_PER_HOUR");
        updateMachineUseCase.updateMachine(renewSoat, created.id());

        var alertsAfterRenew = activeDocumentAlertsFor(assetId);
        assertThat(alertsAfterRenew)
                .as("La alerta de SOAT debe desaparecer apenas se renueva, sin esperar cron/refresh")
                .isEmpty();

        // Ahora vencer el seguro todo riesgo (el SOAT sigue vigente) → solo debe aparecer esa alerta
        MachineRequest expireInsurance = new MachineRequest(
                "Excavadora E2E", "Distrito", "320D",
                LocalDate.now().plusDays(365),
                "Caterpillar",
                LocalDate.now().minusDays(1),
                "ENG-E2E", "ID-E2E", BigDecimal.TEN, BigDecimal.ONE, "GAL_PER_HOUR");
        updateMachineUseCase.updateMachine(expireInsurance, created.id());

        var alertsAfterInsuranceExpiry = activeDocumentAlertsFor(assetId);
        assertThat(alertsAfterInsuranceExpiry).hasSize(1);
        assertThat(alertsAfterInsuranceExpiry.get(0).getAlertSubtype()).isEqualTo("SEGURO TODO RIESGO");
        assertThat(alertsAfterInsuranceExpiry.get(0).getColorEstado()).isEqualTo("ROJO");
    }

    @Test
    void vehicleSoatDocument_AppearsAndClearsOnUpload() {
        MarcaModeloEntity marca = marcaModeloRepository.save(
                MarcaModeloEntity.builder().descripcion("Marca E2E Alertas").build());
        TipoVehiculoEntity tipo = tipoVehiculoRepository.save(
                TipoVehiculoEntity.builder().nombreTipo("AUTOMOVIL").activo(true).build());
        VehicleEntity vehiculo = vehicleRepository.save(
                VehicleEntity.builder()
                        .placa("E2E-ALERT")
                        .idMarca(marca.getIdMarca())
                        .idTipoVehiculo(tipo.getId())
                        .tipoVehiculo(tipo)
                        .kilometrajeActual(100)
                        .activo(true)
                        .build());

        // Subir SOAT ya vencido
        vehiculoInspectionService.saveDocument(
                new VehicleDocumentRequest(vehiculo.getIdVehiculo(), "SOAT", LocalDate.now().minusDays(3), null, null),
                "admin");

        var alertsAfterUpload = activeAlertsFor("E2E-ALERT");
        assertThat(alertsAfterUpload)
                .as("Debe crearse la alerta de SOAT vencido apenas se sube el documento")
                .anySatisfy(a -> {
                    assertThat(a.getAlertSubtype()).isEqualTo("SOAT");
                    assertThat(a.getColorEstado()).isEqualTo("ROJO");
                });

        // Subir un SOAT renovado (fecha lejana) → la alerta debe desaparecer de inmediato
        vehiculoInspectionService.saveDocument(
                new VehicleDocumentRequest(vehiculo.getIdVehiculo(), "SOAT", LocalDate.now().plusDays(365), null, null),
                "admin");

        var alertsAfterRenewal = activeDocumentAlertsFor("E2E-ALERT");
        assertThat(alertsAfterRenewal)
                .as("La alerta de SOAT debe desaparecer apenas se sube el documento renovado")
                .isEmpty();
    }

    @Test
    void userLicense_ExpiredGeneratesAlert_RenewalClearsIt() throws java.net.URISyntaxException {
        CreateUserResponse created = createUserUseCase.createUser(new CreateUserRequest(
                "e2e.licencia", "Usuario E2E", "OPERARIO", "e2e.licencia@example.com", "password123",
                "C1", LocalDate.now().minusDays(10))); // licencia YA vencida

        var alertsAfterCreate = activeAlertsFor("e2e.licencia");
        assertThat(alertsAfterCreate)
                .as("Una licencia YA vencida debe generar alerta ROJA (antes se filtraba y no generaba nada)")
                .anySatisfy(a -> {
                    assertThat(a.getAlertType()).isEqualTo("DOCUMENTO");
                    assertThat(a.getAlertSubtype()).isEqualTo("LICENCIA");
                    assertThat(a.getColorEstado()).isEqualTo("ROJO");
                });

        // Renovar la licencia a futuro lejano → la alerta debe desaparecer de inmediato
        updateUserUseCase.updateUser(new UpdateUserRequest(
                created.id(), null, null, null, null, null, null, LocalDate.now().plusDays(365)));

        var alertsAfterRenewal = activeAlertsFor("e2e.licencia");
        assertThat(alertsAfterRenewal)
                .as("La alerta de licencia debe desaparecer apenas se renueva, sin esperar cron/refresh")
                .isEmpty();
    }
}

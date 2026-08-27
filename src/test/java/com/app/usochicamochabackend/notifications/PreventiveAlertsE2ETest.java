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
import com.app.usochicamochabackend.auth.infrastructure.entity.UserEntity;
import com.app.usochicamochabackend.auth.infrastructure.repository.UserRepositoryJpa;
import com.app.usochicamochabackend.review.infrastructure.entity.InspectionEntity;
import com.app.usochicamochabackend.review.infrastructure.repository.InspectionRepository;
import com.app.usochicamochabackend.update.application.dto.PerformChangeMotorOilRequest;
import com.app.usochicamochabackend.update.application.dto.VehicleOilChangeRequest;
import com.app.usochicamochabackend.update.application.service.OilChangeService;
import com.app.usochicamochabackend.update.application.service.VehicleOilChangeService;
import com.app.usochicamochabackend.update.infrastructure.repository.OilChangeRepository;
import com.app.usochicamochabackend.update.infrastructure.repository.VehicleOilChangeRepository;
import com.app.usochicamochabackend.vehicle.application.dto.VehicleRequest;
import com.app.usochicamochabackend.vehicle.application.service.VehicleService;
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
    @Autowired private com.app.usochicamochabackend.notifications.application.PreventiveAlertCalculationService preventiveAlertCalculationService;

    @Autowired private CreateUserUseCase createUserUseCase;
    @Autowired private UpdateUserUseCase updateUserUseCase;
    @Autowired private UserRepositoryJpa userRepository;

    @Autowired private VehiculoInspectionService vehiculoInspectionService;
    @Autowired private VehicleService vehicleService;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private MarcaModeloRepository marcaModeloRepository;
    @Autowired private TipoVehiculoRepository tipoVehiculoRepository;
    @Autowired private DocumentacionYElementosRepository documentacionRepository;

    @Autowired private VehicleOilChangeService vehicleOilChangeService;
    @Autowired private VehicleOilChangeRepository vehicleOilChangeRepository;
    @Autowired private OilChangeService oilChangeService;
    @Autowired private OilChangeRepository oilChangeRepository;
    @Autowired private InspectionRepository inspectionRepository;

    @Autowired private PreventiveAlertRepository alertRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        alertRepository.deleteAll();
        documentacionRepository.deleteAll();
        vehicleOilChangeRepository.deleteAll();
        oilChangeRepository.deleteAll();
        inspectionRepository.deleteAll();
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

    /** Alertas ROJO/AMARILLO de cambio de aceite (excluye las VERDE informativas de "primer cambio"). */
    private List<com.app.usochicamochabackend.notifications.infrastructure.entity.PreventiveAlertEntity> activeOilAlertsFor(String assetId) {
        return activeAlertsFor(assetId).stream()
                .filter(a -> a.getAlertType() != null && a.getAlertType().startsWith("OIL_CHANGE"))
                .filter(a -> !"VERDE".equals(a.getColorEstado()))
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
                "ENG-E2E", "ID-E2E");

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
                "ENG-E2E", "ID-E2E");
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
                "ENG-E2E", "ID-E2E");
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

    @Test
    void vehicleOilChange_CreatesAndClearsAlertImmediately() {
        MarcaModeloEntity marca = marcaModeloRepository.save(
                MarcaModeloEntity.builder().descripcion("Marca E2E Aceite").build());
        TipoVehiculoEntity tipo = tipoVehiculoRepository.save(
                TipoVehiculoEntity.builder().nombreTipo("AUTOMOVIL").activo(true).build());
        vehicleRepository.save(
                VehicleEntity.builder()
                        .placa("E2E-OIL")
                        .idMarca(marca.getIdMarca())
                        .idTipoVehiculo(tipo.getId())
                        .tipoVehiculo(tipo)
                        .kilometrajeActual(5000)
                        .activo(true)
                        .build());

        // Registrar un cambio "viejo": intervalo de 1000km desde km 0, con el vehículo ya en 5000km
        // → muy por encima del intervalo, debe generar alerta ROJA de inmediato.
        vehicleOilChangeService.registerChange(new VehicleOilChangeRequest(
                "E2E-OIL", null, "MOTOR", null, 4.0, 0, 1000, false));

        var alertsAfterOldChange = activeOilAlertsFor("E2E-OIL");
        assertThat(alertsAfterOldChange)
                .as("Debe crearse la alerta ROJA de cambio de aceite apenas se registra el cambio vencido")
                .anySatisfy(a -> {
                    assertThat(a.getAlertType()).isEqualTo("OIL_CHANGE_VEHICLE");
                    assertThat(a.getColorEstado()).isEqualTo("ROJO");
                });

        // Registrar un cambio fresco al km actual, con intervalo amplio → debe limpiar la alerta ya mismo.
        vehicleOilChangeService.registerChange(new VehicleOilChangeRequest(
                "E2E-OIL", null, "MOTOR", null, 4.0, 5000, 10000, false));

        var alertsAfterFreshChange = activeOilAlertsFor("E2E-OIL");
        assertThat(alertsAfterFreshChange)
                .as("La alerta de cambio de aceite debe desaparecer apenas se registra el cambio nuevo")
                .isEmpty();
    }

    @Test
    void vehicleKilometrajeEdit_TriggersImmediateRecalculation() {
        MarcaModeloEntity marca = marcaModeloRepository.save(
                MarcaModeloEntity.builder().descripcion("Marca E2E Km").build());
        TipoVehiculoEntity tipo = tipoVehiculoRepository.save(
                TipoVehiculoEntity.builder().nombreTipo("AUTOMOVIL").activo(true).build());
        VehicleEntity vehiculo = vehicleRepository.save(
                VehicleEntity.builder()
                        .placa("E2E-KMEDIT")
                        .idMarca(marca.getIdMarca())
                        .idTipoVehiculo(tipo.getId())
                        .tipoVehiculo(tipo)
                        .kilometrajeActual(100)
                        .activo(true)
                        .build());

        // Baseline: último cambio en km 0, intervalo 1000km. Con el vehículo en 100km, todavía va bien.
        vehicleOilChangeService.registerChange(new VehicleOilChangeRequest(
                "E2E-KMEDIT", null, "MOTOR", null, 4.0, 0, 1000, false));
        assertThat(activeOilAlertsFor("E2E-KMEDIT")).isEmpty();

        // Editar el vehículo desde el panel admin subiendo el kilometraje muy por encima del intervalo
        // (esto es justo lo que preguntó el usuario: "si edito el vehículo con un nuevo kilometraje").
        vehicleService.updateVehicle(vehiculo.getIdVehiculo(), new VehicleRequest(
                "E2E-KMEDIT", marca.getIdMarca(), tipo.getId(), 5000,
                "Distrito", null, true));

        var alertsAfterEdit = activeOilAlertsFor("E2E-KMEDIT");
        assertThat(alertsAfterEdit)
                .as("Editar el kilometraje del vehículo debe recalcular la alerta de aceite de inmediato")
                .anySatisfy(a -> {
                    assertThat(a.getAlertType()).isEqualTo("OIL_CHANGE_VEHICLE");
                    assertThat(a.getColorEstado()).isEqualTo("ROJO");
                });
    }

    @Test
    void machineOilChange_CreatesAndClearsAlertImmediately() {
        com.app.usochicamochabackend.auth.application.dto.UserPrincipal principal =
                new com.app.usochicamochabackend.auth.application.dto.UserPrincipal(1L, "admin");
        org.springframework.security.core.Authentication auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        principal, null, java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            MachineRequest machineReq = new MachineRequest(
                    "Máquina E2E Aceite", "Distrito", "320D", null, "Caterpillar", null,
                    "ENG-OIL", "ID-OIL");
            MachineResponse machine = createMachineUseCase.createMachine(machineReq);
            var machineEntity = machineRepository.findById(machine.id()).orElseThrow();

            UserEntity user = userRepository.save(UserEntity.builder()
                    .username("e2e.oil.machine").fullName("E2E").status(true)
                    .email("e2e.oil.machine@example.com").role("ADMIN").password("x").build());

            // Primera inspección a horómetro bajo (100h), usada como línea base para las alertas
            // preventivas de este test (performMotorOilChange ya no exige currentHourMeter >= última inspección).
            inspectionRepository.save(InspectionEntity.builder()
                    .UUID(java.util.UUID.randomUUID().toString())
                    .dateStamp(java.time.LocalDateTime.now().minusDays(30))
                    .hourMeter(100.0)
                    .machine(machineEntity)
                    .user(user)
                    .build());

            // Cambio de aceite en ese momento (100h, intervalo 50h) → línea base recién puesta,
            // uso 0% todavía: no debe haber alerta (esto es esperado, no se afirma explícitamente).
            oilChangeService.performMotorOilChange(new PerformChangeMotorOilRequest(
                    machine.id(), java.time.LocalDateTime.now().minusDays(30), null, 4.0, 100.0, 50));

            // Pasa el tiempo: una inspección posterior reporta la máquina muy por encima del
            // intervalo desde el último cambio (100h + 50h de intervalo, ahora en 5000h) →
            // recalculando (InspectionService también dispara esto, ver test de inspección),
            // debe quedar ROJO.
            inspectionRepository.save(InspectionEntity.builder()
                    .UUID(java.util.UUID.randomUUID().toString())
                    .dateStamp(java.time.LocalDateTime.now())
                    .hourMeter(5000.0)
                    .machine(machineEntity)
                    .user(user)
                    .build());
            preventiveAlertCalculationService.calculateAndEmitAlerts();

            var alertsBeforeNewChange = activeOilAlertsFor(machine.name());
            assertThat(alertsBeforeNewChange)
                    .as("Sanity check: debe haber alerta ROJA antes de registrar el nuevo cambio")
                    .anySatisfy(a -> {
                        assertThat(a.getAlertType()).isEqualTo("OIL_CHANGE_MACHINE");
                        assertThat(a.getAlertSubtype()).isEqualTo("MOTOR");
                        assertThat(a.getColorEstado()).isEqualTo("ROJO");
                    });

            // Registrar el cambio de aceite ya mismo (a las 5000h, intervalo amplio) debe limpiar
            // la alerta de inmediato, sin esperar cron/refresh.
            oilChangeService.performMotorOilChange(new PerformChangeMotorOilRequest(
                    machine.id(), java.time.LocalDateTime.now(), null, 4.0, 5000.0, 10000));

            var alertsAfterFreshChange = activeOilAlertsFor(machine.name());
            assertThat(alertsAfterFreshChange)
                    .as("La alerta de cambio de aceite de maquinaria debe desaparecer apenas se registra el cambio nuevo")
                    .isEmpty();
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }
}

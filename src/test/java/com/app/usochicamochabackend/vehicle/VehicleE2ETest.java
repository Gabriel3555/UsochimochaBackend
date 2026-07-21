package com.app.usochicamochabackend.vehicle;

import com.app.usochicamochabackend.auth.application.dto.UserPrincipal;
import com.app.usochicamochabackend.catalog.infrastructure.entity.TipoVehiculoEntity;
import com.app.usochicamochabackend.catalog.infrastructure.entity.UbicacionEntity;
import com.app.usochicamochabackend.catalog.infrastructure.repository.TipoVehiculoRepository;
import com.app.usochicamochabackend.catalog.infrastructure.repository.UbicacionRepository;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.MarcaModeloEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.VehicleEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.MarcaModeloRepository;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleRepository;
import com.app.usochicamochabackend.vehicleinspection.application.dto.VehiculoInspectionRequest;
import com.app.usochicamochabackend.vehicleinspection.application.port.CreateVehiculoInspectionUseCase;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.repository.InspPreOperativaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * E2E: simula el flujo de la app móvil para vehículos (no motos).
 * Flujo: consulta de vehículos → validación km → envío de inspección (vía servicio)
 *        → verificación en BD → visualización en panel web administrativo.
 */
@Tag("e2e")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class VehicleE2ETest {

    @Autowired private MockMvc mockMvc;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private UbicacionRepository ubicacionRepository;
    @Autowired private TipoVehiculoRepository tipoVehiculoRepository;
    @Autowired private MarcaModeloRepository marcaModeloRepository;
    @Autowired private InspPreOperativaRepository inspeccionRepository;
    @Autowired private CreateVehiculoInspectionUseCase createVehiculoInspectionUseCase;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Integer testVehiculoId;
    private Integer testUbicacionId;
    private static final String TEST_PLACA = "E2E-VEH";

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        inspeccionRepository.deleteAll();
        vehicleRepository.deleteAll();
        tipoVehiculoRepository.deleteAll();
        ubicacionRepository.deleteAll();
        marcaModeloRepository.deleteAll();
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    @BeforeEach
    void setUp() {
        MarcaModeloEntity marca = marcaModeloRepository.save(
                MarcaModeloEntity.builder().descripcion("Marca E2E Vehiculo").build());

        TipoVehiculoEntity tipo = tipoVehiculoRepository.save(
                TipoVehiculoEntity.builder().nombreTipo("AUTOMOVIL").activo(true).build());

        VehicleEntity vehiculo = vehicleRepository.save(
                VehicleEntity.builder()
                        .placa(TEST_PLACA)
                        .idMarca(marca.getIdMarca())
                        .idTipoVehiculo(tipo.getId())
                        .tipoVehiculo(tipo)
                        .kilometrajeActual(100)
                        .activo(true)
                        .build());
        testVehiculoId = vehiculo.getIdVehiculo();

        UbicacionEntity ubicacion = ubicacionRepository.save(
                UbicacionEntity.builder().nombreUbicacion("Sede E2E").activo(true).build());
        testUbicacionId = ubicacion.getId();
    }

    @Test
    void fullVehicleInspectionFlow_ShouldWork() throws Exception {
        // Paso 1 — Consulta de vehículos activos (simula app móvil o panel web)
        mockMvc.perform(get("/api/v1/vehicle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.placa=='" + TEST_PLACA + "')]").exists());

        // Paso 2 — Validación de kilometraje coherente (km nuevo > km registrado)
        mockMvc.perform(get("/api/v1/vehicle-inspection/validar-kilometraje")
                        .param("placa", TEST_PLACA)
                        .param("kilometraje", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alerta").value(false));

        // Paso 3 — Envío de inspección (simula recepción del payload del móvil en el servicio)
        UserPrincipal inspector = new UserPrincipal(1L, "admin");
        VehiculoInspectionRequest request = new VehiculoInspectionRequest(
                TEST_PLACA, 200, true, "Inspección E2E vehículo OK",
                "Bueno", "Bueno", "Bueno", "Bueno", "Funcionando", "Limpio", "Limpio",
                "Vigente", "Vigente", "Vigente", "Vigente",
                null, null, null, null, null, null, null, null,
                true, true, true, true, true,
                true, true, true, false, true, true,
                testUbicacionId);
        createVehiculoInspectionUseCase.create(request, inspector);

        // Paso 4 — Verificación de persistencia en BD
        var inspeccion = inspeccionRepository.findLatestByVehicleId(testVehiculoId);
        assertThat(inspeccion).isPresent();
        assertThat(inspeccion.get().getKilometrajeReportado()).isEqualTo(200);
        assertThat(inspeccion.get().getObservacionesFinales()).isEqualTo("Inspección E2E vehículo OK");

        // Paso 5 — Visualización en panel web administrativo
        mockMvc.perform(get("/api/v1/vehicle-inspection/reports/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.placa=='" + TEST_PLACA + "')]").exists());
    }
}

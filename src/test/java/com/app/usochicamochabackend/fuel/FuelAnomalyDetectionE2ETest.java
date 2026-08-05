package com.app.usochicamochabackend.fuel;

import com.app.usochicamochabackend.auth.application.dto.UserPrincipal;
import com.app.usochicamochabackend.catalog.infrastructure.entity.TipoVehiculoEntity;
import com.app.usochicamochabackend.catalog.infrastructure.repository.TipoVehiculoRepository;
import com.app.usochicamochabackend.fuel.application.dto.AssetFuelConfigRequest;
import com.app.usochicamochabackend.fuel.application.port.ManageAssetFuelConfigUseCase;
import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelInventoryEntity;
import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelTypesEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.AssetFuelConfigRepository;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelInventoryRepository;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelPurchaseRepository;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelTypesRepository;
import com.app.usochicamochabackend.fuel.infrastructure.repository.RefuelingRecordsRepository;
import com.app.usochicamochabackend.machine.infrastructure.entity.MachineEntity;
import com.app.usochicamochabackend.machine.infrastructure.repository.MachineRepository;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.MarcaModeloEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.VehicleEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.MarcaModeloRepository;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * E2E de las discrepancias/anomalías de tanqueos (Fase 5) y de la factura opcional
 * en BOMBA (V23): factura opcional, precio unitario fuera de rango, cantidad fuera
 * de rango típico por tipo de activo, capacidad de tanque excedida, y edición/
 * eliminación de un tanqueo ya registrado — todo a través de los endpoints HTTP
 * reales, no llamando directo al servicio, para validar el flujo completo tal como
 * lo ejercita el frontend.
 */
@Tag("e2e")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FuelAnomalyDetectionE2ETest {

    @org.junit.jupiter.api.io.TempDir
    static Path uploadsTempDir;

    @DynamicPropertySource
    static void overrideUploadsRoot(DynamicPropertyRegistry registry) {
        registry.add("app.storage.uploads-root", () -> uploadsTempDir.toString());
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private FuelTypesRepository fuelTypesRepository;
    @Autowired private FuelInventoryRepository fuelInventoryRepository;
    @Autowired private FuelPurchaseRepository fuelPurchaseRepository;
    @Autowired private RefuelingRecordsRepository refuelingRecordsRepository;
    @Autowired private AssetFuelConfigRepository assetFuelConfigRepository;
    @Autowired private ManageAssetFuelConfigUseCase manageAssetFuelConfigUseCase;
    @Autowired private MachineRepository machineRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private TipoVehiculoRepository tipoVehiculoRepository;
    @Autowired private MarcaModeloRepository marcaModeloRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Long fuelTypeId;
    private Integer vehiculoId;
    private Integer motoId;

    @BeforeEach
    void setUp() {
        FuelTypesEntity acpm = fuelTypesRepository.save(
                FuelTypesEntity.builder().codigo("ACPM").nombre("ACPM / Diésel").activo(true).build());
        fuelTypeId = acpm.getId();
        fuelInventoryRepository.save(
                FuelInventoryEntity.builder().areaCosto("DISTRITO").fuelTypeId(fuelTypeId)
                        .cantidadDisponible(new BigDecimal("100")).build());

        MarcaModeloEntity marca = marcaModeloRepository.save(MarcaModeloEntity.builder().descripcion("Marca E2E").build());

        TipoVehiculoEntity tipoCamioneta = tipoVehiculoRepository.save(
                TipoVehiculoEntity.builder().nombreTipo("CAMIONETA").activo(true).build());
        VehicleEntity vehiculo = vehicleRepository.save(VehicleEntity.builder()
                .placa("FUEL-E2E-1").idMarca(marca.getIdMarca()).idTipoVehiculo(tipoCamioneta.getId())
                .tipoVehiculo(tipoCamioneta).kilometrajeActual(1000).activo(true).build());
        vehiculoId = vehiculo.getIdVehiculo();

        TipoVehiculoEntity tipoMoto = tipoVehiculoRepository.save(
                TipoVehiculoEntity.builder().nombreTipo("MOTOCICLETA").activo(true).build());
        VehicleEntity moto = vehicleRepository.save(VehicleEntity.builder()
                .placa("FUEL-E2E-2").idMarca(marca.getIdMarca()).idTipoVehiculo(tipoMoto.getId())
                .tipoVehiculo(tipoMoto).kilometrajeActual(500).activo(true).build());
        motoId = moto.getIdVehiculo();
    }

    @AfterEach
    void tearDown() {
        refuelingRecordsRepository.deleteAll();
        fuelPurchaseRepository.deleteAll();
        assetFuelConfigRepository.deleteAll();
        fuelInventoryRepository.deleteAll();
        fuelTypesRepository.deleteAll();

        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        vehicleRepository.deleteAll();
        tipoVehiculoRepository.deleteAll();
        marcaModeloRepository.deleteAll();
        machineRepository.deleteAll();
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    private static RequestPostProcessor as(Long id, String username, String role) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                new UserPrincipal(id, username), null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        return SecurityMockMvcRequestPostProcessors.authentication(auth);
    }

    private MockMultipartHttpServletRequestBuilder buildRefuelingRequest(HttpMethod method, String url,
            Integer vehicleId, Long machineId, String lugar, BigDecimal cantidadGalones, BigDecimal horometroKm,
            BigDecimal precioUnitario, BigDecimal totalIngresado, MockMultipartFile factura) {
        MockMultipartHttpServletRequestBuilder builder = method == null
                ? multipart(url) : multipart(method, url);
        if (vehicleId != null) builder.part(new MockPart("vehicleId", vehicleId.toString().getBytes()));
        if (machineId != null) builder.part(new MockPart("machineId", machineId.toString().getBytes()));
        builder.part(new MockPart("lugar", lugar.getBytes()));
        builder.part(new MockPart("areaCosto", "DISTRITO".getBytes()));
        builder.part(new MockPart("fuelTypeId", fuelTypeId.toString().getBytes()));
        builder.part(new MockPart("cantidadGalones", cantidadGalones.toString().getBytes()));
        builder.part(new MockPart("horometroKm", horometroKm.toString().getBytes()));
        if (precioUnitario != null) builder.part(new MockPart("precioUnitario", precioUnitario.toString().getBytes()));
        if (totalIngresado != null) builder.part(new MockPart("totalIngresado", totalIngresado.toString().getBytes()));
        if (factura != null) builder.file(factura);
        return builder;
    }

    // ---- Factura opcional en BOMBA (V23) ----

    @Test
    void tanqueoBomba_SinFactura_SeRegistraCorrectamente() throws Exception {
        var request = buildRefuelingRequest(null, "/api/v1/fuel/refueling", vehiculoId, null, "BOMBA",
                new BigDecimal("20"), new BigDecimal("1100"), new BigDecimal("10000"), new BigDecimal("200000"), null);

        mockMvc.perform(request.with(as(1L, "operario", "OPERARIO")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.urlFactura").doesNotExist())
                .andExpect(jsonPath("$.discrepanciaValor").value(false));
    }

    // ---- Precio unitario fuera de rango ----

    @Test
    void precioUnitario_MasDe30PorCientoSobreElPromedioReciente_QuedaMarcadoComoAnomalia() throws Exception {
        // Baseline: dos tanqueos BOMBA a precio 10000 -> promedio reciente = 10000.
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(buildRefuelingRequest(null, "/api/v1/fuel/refueling", vehiculoId, null, "BOMBA",
                            new BigDecimal("10"), new BigDecimal("1000"), new BigDecimal("10000"), new BigDecimal("100000"), null)
                    .with(as(1L, "operario", "OPERARIO")))
                    .andExpect(status().isCreated());
        }

        // Tercero a 15000 (50% por encima del promedio) -> fuera de rango.
        mockMvc.perform(buildRefuelingRequest(null, "/api/v1/fuel/refueling", vehiculoId, null, "BOMBA",
                        new BigDecimal("10"), new BigDecimal("1000"), new BigDecimal("15000"), new BigDecimal("150000"), null)
                .with(as(1L, "operario", "OPERARIO")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.precioFueraDeRango").value(true));
    }

    @Test
    void precioUnitario_DentroDelRango_NoQuedaMarcado() throws Exception {
        mockMvc.perform(buildRefuelingRequest(null, "/api/v1/fuel/refueling", vehiculoId, null, "BOMBA",
                        new BigDecimal("10"), new BigDecimal("1000"), new BigDecimal("10000"), new BigDecimal("100000"), null)
                .with(as(1L, "operario", "OPERARIO")))
                .andExpect(status().isCreated());

        // Segundo a 10800 (8% por encima) -> dentro del 30% de tolerancia.
        mockMvc.perform(buildRefuelingRequest(null, "/api/v1/fuel/refueling", vehiculoId, null, "BOMBA",
                        new BigDecimal("10"), new BigDecimal("1000"), new BigDecimal("10800"), new BigDecimal("108000"), null)
                .with(as(1L, "operario", "OPERARIO")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.precioFueraDeRango").value(false));
    }

    // ---- Cantidad fuera de rango típico por tipo de activo ----

    @Test
    void cantidadFueraDeRango_VehiculoNoMoto_MasDe60Galones_QuedaMarcado() throws Exception {
        mockMvc.perform(buildRefuelingRequest(null, "/api/v1/fuel/refueling", vehiculoId, null, "ALMACEN",
                        new BigDecimal("70"), new BigDecimal("1000"), null, null, null)
                .with(as(1L, "operario", "OPERARIO")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cantidadFueraDeRango").value(true));
    }

    @Test
    void cantidadDentroDeRango_VehiculoNoMoto_MenosDe60Galones_NoQuedaMarcado() throws Exception {
        mockMvc.perform(buildRefuelingRequest(null, "/api/v1/fuel/refueling", vehiculoId, null, "ALMACEN",
                        new BigDecimal("30"), new BigDecimal("1000"), null, null, null)
                .with(as(1L, "operario", "OPERARIO")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cantidadFueraDeRango").value(false));
    }

    @Test
    void cantidadFueraDeRango_Motocicleta_MasDe15Galones_QuedaMarcado() throws Exception {
        mockMvc.perform(buildRefuelingRequest(null, "/api/v1/fuel/refueling", motoId, null, "ALMACEN",
                        new BigDecimal("20"), new BigDecimal("500"), null, null, null)
                .with(as(1L, "operario", "OPERARIO")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cantidadFueraDeRango").value(true));
    }

    @Test
    void cantidadFueraDeRango_Maquinaria_MasDe500Galones_QuedaMarcado() throws Exception {
        MachineEntity maquina = machineRepository.save(
                MachineEntity.builder().name("Retro E2E").belongsTo("Distrito").model("2020").status(true).build());

        mockMvc.perform(buildRefuelingRequest(null, "/api/v1/fuel/refueling", null, maquina.getId(), "ALMACEN",
                        new BigDecimal("600"), new BigDecimal("50"), null, null, null)
                .with(as(1L, "operario", "OPERARIO")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cantidadFueraDeRango").value(true));
    }

    // ---- Capacidad de tanque excedida (cruce con asset_fuel_config) ----

    @Test
    void capacidadDeTanqueExcedida_CruzaConAssetFuelConfig() throws Exception {
        manageAssetFuelConfigUseCase.configurarVehiculo(vehiculoId,
                new AssetFuelConfigRequest(fuelTypeId, new BigDecimal("0.2"), "KM_POR_GALON", new BigDecimal("50")));

        // 55 gal > 50 gal configurados -> excede capacidad, aunque esté bajo el umbral genérico de 60.
        mockMvc.perform(buildRefuelingRequest(null, "/api/v1/fuel/refueling", vehiculoId, null, "ALMACEN",
                        new BigDecimal("55"), new BigDecimal("1000"), null, null, null)
                .with(as(1L, "operario", "OPERARIO")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.capacidadExcedida").value(true))
                .andExpect(jsonPath("$.cantidadFueraDeRango").value(false));
    }

    // ---- Editar y eliminar un tanqueo ya registrado ----

    @Test
    void editarTanqueoAlmacen_CambiaCantidadSinTocarInventario() throws Exception {
        var creado = mockMvc.perform(buildRefuelingRequest(null, "/api/v1/fuel/refueling", vehiculoId, null, "ALMACEN",
                        new BigDecimal("30"), new BigDecimal("1000"), null, null, null)
                        .with(as(1L, "operario", "OPERARIO")))
                .andExpect(status().isCreated())
                .andReturn();
        Long id = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(creado.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(buildRefuelingRequest(HttpMethod.PUT, "/api/v1/fuel/refueling/" + id, vehiculoId, null, "ALMACEN",
                        new BigDecimal("20"), new BigDecimal("1000"), null, null, null)
                        .with(as(1L, "admin", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadGalones").value(20));

        // El módulo no lleva control de inventario de almacén (decisión de alcance
        // 28/07/2026): el saldo sembrado en setUp() (100) queda intacto.
        FuelInventoryEntity trasEditar = fuelInventoryRepository.findByAreaCostoAndFuelTypeId("DISTRITO", fuelTypeId).orElseThrow();
        assertThat(trasEditar.getCantidadDisponible()).isEqualByComparingTo("100");
    }

    @Test
    void eliminarTanqueoAlmacen_SoftDeleteSinTocarInventario() throws Exception {
        var creado = mockMvc.perform(buildRefuelingRequest(null, "/api/v1/fuel/refueling", vehiculoId, null, "ALMACEN",
                        new BigDecimal("25"), new BigDecimal("1000"), null, null, null)
                        .with(as(1L, "operario", "OPERARIO")))
                .andExpect(status().isCreated())
                .andReturn();
        Long id = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(creado.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/v1/fuel/refueling/" + id)
                        .with(as(1L, "admin", "ADMIN")))
                .andExpect(status().isNoContent());

        FuelInventoryEntity trasEliminar = fuelInventoryRepository.findByAreaCostoAndFuelTypeId("DISTRITO", fuelTypeId).orElseThrow();
        assertThat(trasEliminar.getCantidadDisponible()).isEqualByComparingTo("100");

        mockMvc.perform(get("/api/v1/fuel/refueling").param("activo", "true").with(as(2L, "operario", "OPERARIO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }
}

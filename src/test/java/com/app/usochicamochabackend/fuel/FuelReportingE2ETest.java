package com.app.usochicamochabackend.fuel;

import com.app.usochicamochabackend.auth.application.dto.UserPrincipal;
import com.app.usochicamochabackend.fuel.application.dto.AssetFuelConfigRequest;
import com.app.usochicamochabackend.fuel.application.dto.FuelDashboardResponse;
import com.app.usochicamochabackend.fuel.application.dto.FuelDistributionResponse;
import com.app.usochicamochabackend.fuel.application.dto.FuelPerformanceResponse;
import com.app.usochicamochabackend.fuel.application.dto.FuelPurchaseRequest;
import com.app.usochicamochabackend.fuel.application.dto.FuelReintegrationRequest;
import com.app.usochicamochabackend.fuel.application.dto.RefuelingRecordRequest;
import com.app.usochicamochabackend.fuel.application.port.ManageAssetFuelConfigUseCase;
import com.app.usochicamochabackend.fuel.application.port.RegisterFuelPurchaseUseCase;
import com.app.usochicamochabackend.fuel.application.port.RegisterFuelReintegrationUseCase;
import com.app.usochicamochabackend.fuel.application.port.RegisterRefuelingRecordUseCase;
import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelInventoryEntity;
import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelTypesEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.AssetFuelConfigRepository;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelInventoryRepository;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelPurchaseRepository;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelReintegrationsRepository;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelTypesRepository;
import com.app.usochicamochabackend.fuel.infrastructure.repository.RefuelingRecordsRepository;
import com.app.usochicamochabackend.machine.infrastructure.entity.MachineEntity;
import com.app.usochicamochabackend.machine.infrastructure.repository.MachineRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * E2E de Fase 2-3 del módulo combustibles: dashboard financiero, control de almacén,
 * configuración de rendimiento, reintegros, rendimiento operativo y distribución.
 */
@Tag("e2e")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FuelReportingE2ETest {

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
    @Autowired private FuelReintegrationsRepository fuelReintegrationsRepository;
    @Autowired private AssetFuelConfigRepository assetFuelConfigRepository;
    @Autowired private MachineRepository machineRepository;
    @Autowired private RegisterFuelPurchaseUseCase registerFuelPurchaseUseCase;
    @Autowired private RegisterRefuelingRecordUseCase registerRefuelingRecordUseCase;
    @Autowired private RegisterFuelReintegrationUseCase registerFuelReintegrationUseCase;
    @Autowired private ManageAssetFuelConfigUseCase manageAssetFuelConfigUseCase;

    private Long fuelTypeId;

    @BeforeEach
    void setUp() {
        FuelTypesEntity acpm = fuelTypesRepository.save(
                FuelTypesEntity.builder().codigo("ACPM").nombre("ACPM / Diésel").activo(true).build());
        fuelTypeId = acpm.getId();
        fuelInventoryRepository.save(
                FuelInventoryEntity.builder().areaCosto("DISTRITO").fuelTypeId(fuelTypeId).cantidadDisponible(BigDecimal.ZERO).build());
    }

    @AfterEach
    void tearDown() {
        fuelReintegrationsRepository.deleteAll();
        refuelingRecordsRepository.deleteAll();
        fuelPurchaseRepository.deleteAll();
        assetFuelConfigRepository.deleteAll();
        fuelInventoryRepository.deleteAll();
        fuelTypesRepository.deleteAll();
        machineRepository.deleteAll();
    }

    private static RequestPostProcessor as(Long id, String username, String role) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                new UserPrincipal(id, username), null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        return SecurityMockMvcRequestPostProcessors.authentication(auth);
    }

    private void registrarCompra(BigDecimal cantidad, BigDecimal precioUnitario) {
        MultipartFile factura = new MockMultipartFile("factura", "f.pdf", "application/pdf", new byte[]{1, 2, 3});
        FuelPurchaseRequest request = new FuelPurchaseRequest(
                "DISTRITO", fuelTypeId, cantidad, precioUnitario, null, cantidad.multiply(precioUnitario));
        registerFuelPurchaseUseCase.registrar(request, factura, 1L);
    }

    @Test
    void dashboardFinanciero_ReflejaComprasYTanqueoBomba() throws Exception {
        registrarCompra(new BigDecimal("100"), new BigDecimal("10000")); // 1_000_000 en compras

        MultipartFile factura = new MockMultipartFile("factura", "f.pdf", "application/pdf", new byte[]{1, 2, 3});
        RefuelingRecordRequest tanqueoBomba = new RefuelingRecordRequest(
                3, null, "BOMBA", "DISTRITO", fuelTypeId, new BigDecimal("20"), new BigDecimal("500"),
                false, new BigDecimal("10000"), null, new BigDecimal("200000"), "Estación Norte");
        registerRefuelingRecordUseCase.registrar(tanqueoBomba, factura, 2L); // 200_000 en tanqueo bomba

        mockMvc.perform(get("/api/v1/fuel/dashboard/financiero")
                        .param("fechaInicio", LocalDate.now().withDayOfMonth(1).toString())
                        .param("fechaFin", LocalDate.now().toString())
                        .with(as(1L, "supervisor", "SUPERVISOR_OPERATIVO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gastoBruto").value(1200000))
                .andExpect(jsonPath("$.gastoNeto").value(1200000));
    }

    @Test
    void controlDeAlmacen_ConciliacionCalculaSaldoInicialCorrectamente() throws Exception {
        registrarCompra(new BigDecimal("100"), new BigDecimal("10000"));

        RefuelingRecordRequest tanqueoAlmacen = new RefuelingRecordRequest(
                null, 99L, "ALMACEN", "DISTRITO", fuelTypeId, new BigDecimal("30"), new BigDecimal("500"),
                false, null, null, null, "Bodega");
        registerRefuelingRecordUseCase.registrar(tanqueoAlmacen, null, 2L);

        mockMvc.perform(get("/api/v1/fuel/almacen/movimientos")
                        .param("fechaInicio", LocalDate.now().withDayOfMonth(1).toString())
                        .param("fechaFin", LocalDate.now().toString())
                        .with(as(1L, "almacen", "ALMACEN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conciliacion[0].saldoInicial").value(0))
                .andExpect(jsonPath("$.conciliacion[0].entradas").value(100))
                .andExpect(jsonPath("$.conciliacion[0].salidas").value(30))
                .andExpect(jsonPath("$.conciliacion[0].saldoFinal").value(70));
    }

    @Test
    void rendimiento_ConConfigYTanqueoPrevio_CalculaProyectadoYExcluyeActivosSinLineaBase() throws Exception {
        MachineEntity maquina = machineRepository.save(MachineEntity.builder()
                .name("Retroexcavadora E2E").belongsTo("Distrito").model("2020").build());

        manageAssetFuelConfigUseCase.configurarMaquina(maquina.getId(),
                new AssetFuelConfigRequest(fuelTypeId, new BigDecimal("0.5"), "GAL_POR_HORA", new BigDecimal("50")));

        RefuelingRecordRequest primerTanqueo = new RefuelingRecordRequest(
                null, maquina.getId(), "ALMACEN", "DISTRITO", fuelTypeId, new BigDecimal("10"), new BigDecimal("100"),
                false, null, null, null, null);
        registrarCompra(new BigDecimal("50"), new BigDecimal("10000"));
        registerRefuelingRecordUseCase.registrar(primerTanqueo, null, 2L);

        RefuelingRecordRequest segundoTanqueo = new RefuelingRecordRequest(
                null, maquina.getId(), "ALMACEN", "DISTRITO", fuelTypeId, new BigDecimal("25"), new BigDecimal("150"),
                false, null, null, null, null);
        registerRefuelingRecordUseCase.registrar(segundoTanqueo, null, 2L);

        // El primer tanqueo no tiene línea base (no hay uno anterior) -> excluido.
        // El segundo sí: 50 horas ejecutadas * 0.5 gal/hora = 25 galones proyectados; real 25 -> sin desviación.
        var response = mockMvc.perform(get("/api/v1/fuel/rendimiento")
                        .param("tipo", "MAQUINARIA")
                        .with(as(1L, "supervisor", "SUPERVISOR_OPERATIVO")))
                .andExpect(status().isOk())
                .andReturn();

        List<FuelPerformanceResponse> body = new com.fasterxml.jackson.databind.ObjectMapper()
                .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .readValue(response.getResponse().getContentAsString(),
                        new com.fasterxml.jackson.core.type.TypeReference<List<FuelPerformanceResponse>>() {});

        assertThat(body).hasSize(1);
        assertThat(body.get(0).galonesProyectados()).isEqualByComparingTo("25");
        assertThat(body.get(0).alerta()).isFalse();
    }

    @Test
    void reintegroDeTanqueoAlmacen_DevuelveInventarioYApareceEnDistribucionSinValorizar() throws Exception {
        registrarCompra(new BigDecimal("100"), new BigDecimal("10000"));

        RefuelingRecordRequest tanqueoAlmacen = new RefuelingRecordRequest(
                null, 99L, "ALMACEN", "DISTRITO", fuelTypeId, new BigDecimal("30"), new BigDecimal("500"),
                false, null, null, null, null);
        var tanqueo = registerRefuelingRecordUseCase.registrar(tanqueoAlmacen, null, 2L);

        registerFuelReintegrationUseCase.registrar(new FuelReintegrationRequest(tanqueo.id(), new BigDecimal("5")), 2L);

        FuelInventoryEntity inventario = fuelInventoryRepository.findByAreaCostoAndFuelTypeId("DISTRITO", fuelTypeId).orElseThrow();
        assertThat(inventario.getCantidadDisponible()).isEqualByComparingTo("75"); // 100 - 30 + 5

        mockMvc.perform(get("/api/v1/fuel/distribucion")
                        .param("area", "DISTRITO")
                        .with(as(1L, "supervisor", "SUPERVISOR_OPERATIVO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filas[0].valorDespachado").doesNotExist())
                .andExpect(jsonPath("$.filas[0].cantidadReintegrada").value(5));
    }
}

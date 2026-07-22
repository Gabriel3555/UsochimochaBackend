package com.app.usochicamochabackend.fuel;

import com.app.usochicamochabackend.auth.application.dto.UserPrincipal;
import com.app.usochicamochabackend.fuel.application.dto.FuelPurchaseRequest;
import com.app.usochicamochabackend.fuel.application.dto.RefuelingRecordRequest;
import com.app.usochicamochabackend.fuel.application.port.AdjustFuelInventoryUseCase;
import com.app.usochicamochabackend.fuel.application.port.RegisterFuelPurchaseUseCase;
import com.app.usochicamochabackend.fuel.application.port.RegisterRefuelingRecordUseCase;
import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelInventoryEntity;
import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelTypesEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelInventoryRepository;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelPurchaseRepository;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelTypesRepository;
import com.app.usochicamochabackend.fuel.infrastructure.repository.RefuelingRecordsRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * E2E de Fase 0-1 del módulo combustibles: fundaciones de datos + tanqueo/suministro de almacén.
 */
@Tag("e2e")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FuelModuleE2ETest {

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
    @Autowired private RegisterFuelPurchaseUseCase registerFuelPurchaseUseCase;
    @Autowired private RegisterRefuelingRecordUseCase registerRefuelingRecordUseCase;
    @Autowired private AdjustFuelInventoryUseCase adjustFuelInventoryUseCase;

    private Long fuelTypeId;

    @BeforeEach
    void setUp() {
        FuelTypesEntity acpm = fuelTypesRepository.save(
                FuelTypesEntity.builder().codigo("ACPM").nombre("ACPM / Diésel").activo(true).build());
        fuelTypeId = acpm.getId();
        fuelInventoryRepository.save(
                FuelInventoryEntity.builder()
                        .areaCosto("DISTRITO")
                        .fuelTypeId(fuelTypeId)
                        .cantidadDisponible(BigDecimal.ZERO)
                        .build());
    }

    @AfterEach
    void tearDown() {
        refuelingRecordsRepository.deleteAll();
        fuelPurchaseRepository.deleteAll();
        fuelInventoryRepository.deleteAll();
        fuelTypesRepository.deleteAll();
    }

    private static RequestPostProcessor as(Long id, String username, String... roles) {
        List<GrantedAuthority> authorities = Arrays.stream(roles)
                .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r))
                .collect(Collectors.toList());
        Authentication auth = new UsernamePasswordAuthenticationToken(new UserPrincipal(id, username), null, authorities);
        return SecurityMockMvcRequestPostProcessors.authentication(auth);
    }

    @Test
    void flujoFeliz_CompraYTanqueoAlmacen_ActualizanInventarioYQuedanEnHistorial() throws Exception {
        // Paso 1: compra de 100 gal ACPM para DISTRITO, vía HTTP multipart real (SUPERVISOR_OPERATIVO)
        mockMvc.perform(multipart("/api/v1/fuel/purchases")
                        .part(new org.springframework.mock.web.MockPart("areaCosto", "DISTRITO".getBytes()))
                        .part(new org.springframework.mock.web.MockPart("fuelTypeId", fuelTypeId.toString().getBytes()))
                        .part(new org.springframework.mock.web.MockPart("cantidad", "100".getBytes()))
                        .part(new org.springframework.mock.web.MockPart("precioUnitario", "10000".getBytes()))
                        .part(new org.springframework.mock.web.MockPart("totalIngresado", "1000000".getBytes()))
                        .file(new MockMultipartFile("factura", "f.pdf", MediaType.APPLICATION_PDF_VALUE, new byte[]{1, 2, 3}))
                        .with(as(1L, "supervisor", "SUPERVISOR_OPERATIVO")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.discrepanciaValor").value(false));

        FuelInventoryEntity trasCompra = fuelInventoryRepository.findByAreaCostoAndFuelTypeId("DISTRITO", fuelTypeId).orElseThrow();
        assertThat(trasCompra.getCantidadDisponible()).isEqualByComparingTo("100");

        // Paso 2: tanqueo ALMACEN de 30 gal a una máquina (OPERARIO)
        RefuelingRecordRequest tanqueo = new RefuelingRecordRequest(
                null, 99L, "ALMACEN", "DISTRITO", fuelTypeId, new BigDecimal("30"), new BigDecimal("500"),
                false, null, null, null, "Bodega central");
        registerRefuelingRecordUseCase.registrar(tanqueo, null, 2L);

        FuelInventoryEntity trasTanqueo = fuelInventoryRepository.findByAreaCostoAndFuelTypeId("DISTRITO", fuelTypeId).orElseThrow();
        assertThat(trasTanqueo.getCantidadDisponible()).isEqualByComparingTo("70");

        // Paso 3: el historial de tanqueos refleja el registro
        mockMvc.perform(get("/api/v1/fuel/refueling").with(as(2L, "operario", "OPERARIO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].machineId").value(99));
    }

    @Test
    void tanqueoAlmacenSinStockSuficiente_Retorna409YNoModificaInventario() {
        RefuelingRecordRequest tanqueo = new RefuelingRecordRequest(
                null, 99L, "ALMACEN", "DISTRITO", fuelTypeId, new BigDecimal("30"), new BigDecimal("500"),
                false, null, null, null, null);

        assertThrows(ResponseStatusException.class, () -> registerRefuelingRecordUseCase.registrar(tanqueo, null, 2L));

        FuelInventoryEntity inventario = fuelInventoryRepository.findByAreaCostoAndFuelTypeId("DISTRITO", fuelTypeId).orElseThrow();
        assertThat(inventario.getCantidadDisponible()).isEqualByComparingTo("0");
        assertThat(refuelingRecordsRepository.findAll()).isEmpty();
    }

    @Test
    void compraConDiscrepanciaMayorAlUnoPorciento_QuedaMarcadaEnLaRespuestaHTTP() throws Exception {
        // totalCalculado = 100*10000 = 1_000_000; totalIngresado con 5% de diferencia
        mockMvc.perform(multipart("/api/v1/fuel/purchases")
                        .part(new org.springframework.mock.web.MockPart("areaCosto", "DISTRITO".getBytes()))
                        .part(new org.springframework.mock.web.MockPart("fuelTypeId", fuelTypeId.toString().getBytes()))
                        .part(new org.springframework.mock.web.MockPart("cantidad", "100".getBytes()))
                        .part(new org.springframework.mock.web.MockPart("precioUnitario", "10000".getBytes()))
                        .part(new org.springframework.mock.web.MockPart("totalIngresado", "950000".getBytes()))
                        .file(new MockMultipartFile("factura", "f.pdf", MediaType.APPLICATION_PDF_VALUE, new byte[]{1, 2, 3}))
                        .with(as(1L, "supervisor", "SUPERVISOR_OPERATIVO")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.discrepanciaValor").value(true));
    }

    @Test
    void matrizDePermisos_OperarioNoPuedeComprarPeroSiTanquear_AlmacenSoloLee() throws Exception {
        // OPERARIO no puede POST /purchases
        mockMvc.perform(multipart("/api/v1/fuel/purchases")
                        .part(new org.springframework.mock.web.MockPart("areaCosto", "DISTRITO".getBytes()))
                        .part(new org.springframework.mock.web.MockPart("fuelTypeId", fuelTypeId.toString().getBytes()))
                        .part(new org.springframework.mock.web.MockPart("cantidad", "10".getBytes()))
                        .part(new org.springframework.mock.web.MockPart("precioUnitario", "10000".getBytes()))
                        .part(new org.springframework.mock.web.MockPart("totalIngresado", "100000".getBytes()))
                        .file(new MockMultipartFile("factura", "f.pdf", MediaType.APPLICATION_PDF_VALUE, new byte[]{1, 2, 3}))
                        .with(as(3L, "operario", "OPERARIO")))
                .andExpect(status().isForbidden());

        // OPERARIO sí puede POST /refueling (ALMACEN, con stock previamente cargado)
        adjustFuelInventoryUseCase.increment("DISTRITO", fuelTypeId, new BigDecimal("50"));
        mockMvc.perform(multipart("/api/v1/fuel/refueling")
                        .part(new org.springframework.mock.web.MockPart("machineId", "99".getBytes()))
                        .part(new org.springframework.mock.web.MockPart("lugar", "ALMACEN".getBytes()))
                        .part(new org.springframework.mock.web.MockPart("areaCosto", "DISTRITO".getBytes()))
                        .part(new org.springframework.mock.web.MockPart("fuelTypeId", fuelTypeId.toString().getBytes()))
                        .part(new org.springframework.mock.web.MockPart("cantidadGalones", "10".getBytes()))
                        .part(new org.springframework.mock.web.MockPart("horometroKm", "500".getBytes()))
                        .with(as(3L, "operario", "OPERARIO")))
                .andExpect(status().isCreated());

        // ALMACEN puede GET /purchases pero no POST /purchases
        mockMvc.perform(get("/api/v1/fuel/purchases").with(as(4L, "almacen", "ALMACEN")))
                .andExpect(status().isOk());
        mockMvc.perform(multipart("/api/v1/fuel/purchases")
                        .part(new org.springframework.mock.web.MockPart("areaCosto", "DISTRITO".getBytes()))
                        .part(new org.springframework.mock.web.MockPart("fuelTypeId", fuelTypeId.toString().getBytes()))
                        .part(new org.springframework.mock.web.MockPart("cantidad", "10".getBytes()))
                        .part(new org.springframework.mock.web.MockPart("precioUnitario", "10000".getBytes()))
                        .part(new org.springframework.mock.web.MockPart("totalIngresado", "100000".getBytes()))
                        .file(new MockMultipartFile("factura", "f.pdf", MediaType.APPLICATION_PDF_VALUE, new byte[]{1, 2, 3}))
                        .with(as(4L, "almacen", "ALMACEN")))
                .andExpect(status().isForbidden());
    }
}

package com.app.usochicamochabackend.moto;

import com.app.usochicamochabackend.auth.application.dto.UserPrincipal;
import org.junit.jupiter.api.Tag;
import com.app.usochicamochabackend.catalog.infrastructure.entity.TipoVehiculoEntity;
import com.app.usochicamochabackend.catalog.infrastructure.entity.UbicacionEntity;
import com.app.usochicamochabackend.catalog.infrastructure.repository.TipoVehiculoRepository;
import com.app.usochicamochabackend.catalog.infrastructure.repository.UbicacionRepository;
import com.app.usochicamochabackend.moto.application.dto.InspeccionMotoRequest;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.MarcaModeloEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.VehicleEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.MarcaModeloRepository;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleRepository;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.repository.InspPreOperativaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("e2e")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class MotoE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private UbicacionRepository ubicacionRepository;

    @Autowired
    private TipoVehiculoRepository tipoVehiculoRepository;

    @Autowired
    private MarcaModeloRepository marcaModeloRepository;

    @Autowired
    private InspPreOperativaRepository inspeccionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Integer testVehiculoId;
    private Integer testUbicacionId;

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
        MarcaModeloEntity marca = MarcaModeloEntity.builder()
                .descripcion("Marca E2E")
                .build();
        marca = marcaModeloRepository.save(marca);

        TipoVehiculoEntity tipo = TipoVehiculoEntity.builder()
                .nombreTipo("MOTOCICLETA")
                .activo(true)
                .build();
        tipo = tipoVehiculoRepository.save(tipo);

        VehicleEntity moto = VehicleEntity.builder()
                .placa("E2E-123")
                .idMarca(marca.getIdMarca())
                .idTipoVehiculo(tipo.getId())
                .tipoVehiculo(tipo)
                .kilometrajeActual(100)
                .activo(true)
                .build();
        moto = vehicleRepository.save(moto);
        testVehiculoId = moto.getIdVehiculo();

        UbicacionEntity ubicacion = UbicacionEntity.builder()
                .nombreUbicacion("Patio E2E")
                .activo(true)
                .build();
        ubicacion = ubicacionRepository.save(ubicacion);
        testUbicacionId = ubicacion.getId();
    }

    @Test
    void fullMotorcycleInspectionFlow_ShouldWork() throws Exception {
        UserPrincipal principal = new UserPrincipal(1L, "admin");
        var auth = new UsernamePasswordAuthenticationToken(
                principal, null, Set.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(get("/api/v1/moto/placas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.placa=='E2E-123')]").exists());

        mockMvc.perform(get("/api/v1/moto/ubicaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nombreUbicacion=='Patio E2E')]").exists());

        InspeccionMotoRequest request = new InspeccionMotoRequest(
                testVehiculoId,
                150,
                "EXCELENTE",
                "Prueba E2E terminada",
                "Vigente",
                "Vigente",
                "Vigente",
                "N/A",
                "Bueno",
                "Bueno",
                "Bueno",
                testUbicacionId);

        mockMvc.perform(post("/api/v1/moto/inspeccion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        var lastInspection = inspeccionRepository.findLatestByVehicleId(testVehiculoId);
        assertThat(lastInspection).isPresent();
        assertThat(lastInspection.get().getKilometrajeReportado()).isEqualTo(150);
        assertThat(lastInspection.get().getObservacionesFinales()).isEqualTo("Prueba E2E terminada");

        // Paso 4: verificar que la inspección es visible en el panel web administrativo
        mockMvc.perform(get("/api/v1/moto/inspections/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.placa=='E2E-123')]").exists());

        SecurityContextHolder.clearContext();
    }
}

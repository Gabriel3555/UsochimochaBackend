package com.app.usochicamochabackend.moto.application.service;

import com.app.usochicamochabackend.catalog.infrastructure.entity.TipoVehiculoEntity;
import com.app.usochicamochabackend.catalog.infrastructure.repository.TipoVehiculoRepository;
import com.app.usochicamochabackend.exception.VehicleSoftDeletedConflictException;
import com.app.usochicamochabackend.vehicle.application.dto.VehicleRequest;
import com.app.usochicamochabackend.vehicle.application.dto.VehicleResponse;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.VehicleEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MotoServiceSoftDeleteTest {

    @Autowired
    private MotoService motoService;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private TipoVehiculoRepository tipoVehiculoRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setup() {
        // Crear tipo MOTOCICLETA si no existe
        TipoVehiculoEntity tipoMoto = tipoVehiculoRepository.findByNombreTipoIgnoreCase("MOTOCICLETA")
            .orElseGet(() -> tipoVehiculoRepository.save(TipoVehiculoEntity.builder()
                .nombreTipo("MOTOCICLETA")
                .activo(true)
                .build()));

        // Insertar marca
        jdbcTemplate.update(
            "INSERT INTO cat_marcas_modelos (id_marca, descripcion) VALUES (1, 'TEST_MARCA') " +
            "ON CONFLICT DO NOTHING"
        );
    }

    @Test
    void testCreateMotoWithActivePlaqueThrows400() {
        TipoVehiculoEntity tipoMoto = tipoVehiculoRepository.findByNombreTipoIgnoreCase("MOTOCICLETA")
            .orElseThrow();

        VehicleEntity active = vehicleRepository.save(VehicleEntity.builder()
            .placa("MOTO-ACTIVA")
            .idMarca(1)
            .idTipoVehiculo(tipoMoto.getId())
            .tipoVehiculo(tipoMoto)
            .activo(true)
            .build());

        VehicleRequest request = new VehicleRequest(
            "MOTO-ACTIVA",
            1,
            tipoMoto.getId(),
            100,
            "DISTRICT",
            null,
            null,
            null,
            null,
            null
        );

        assertThatThrownBy(() -> motoService.createMoto(request))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Ya existe un vehículo con esta placa");
    }

    @Test
    void testCreateMotoWithSoftDeletedPlaqueThrows409() {
        TipoVehiculoEntity tipoMoto = tipoVehiculoRepository.findByNombreTipoIgnoreCase("MOTOCICLETA")
            .orElseThrow();

        VehicleEntity deleted = vehicleRepository.save(VehicleEntity.builder()
            .placa("MOTO-SOFT-DEL")
            .idMarca(1)
            .idTipoVehiculo(tipoMoto.getId())
            .tipoVehiculo(tipoMoto)
            .activo(false)
            .build());

        VehicleRequest request = new VehicleRequest(
            "MOTO-SOFT-DEL",
            1,
            tipoMoto.getId(),
            100,
            "DISTRICT",
            null,
            null,
            null,
            null,
            null
        );

        assertThatThrownBy(() -> motoService.createMoto(request))
            .isInstanceOf(VehicleSoftDeletedConflictException.class)
            .hasMessageContaining("fue eliminada")
            .satisfies(ex -> {
                VehicleSoftDeletedConflictException conflict = (VehicleSoftDeletedConflictException) ex;
                assertThat(conflict.getSoftDeletedVehicle()).isNotNull();
                assertThat(conflict.getSoftDeletedVehicle().placa()).isEqualTo("MOTO-SOFT-DEL");
                assertThat(conflict.getSuggestedAction()).isEqualTo("restore_or_create_new");
            });
    }

    @Test
    void testRestoreSoftDeletedMotoSucceeds() {
        TipoVehiculoEntity tipoMoto = tipoVehiculoRepository.findByNombreTipoIgnoreCase("MOTOCICLETA")
            .orElseThrow();

        VehicleEntity deleted = vehicleRepository.save(VehicleEntity.builder()
            .placa("MOTO-RESTORE-TEST")
            .idMarca(1)
            .idTipoVehiculo(tipoMoto.getId())
            .tipoVehiculo(tipoMoto)
            .activo(false)
            .build());

        VehicleResponse restored = motoService.restoreMoto(deleted.getIdVehiculo());

        assertThat(restored).isNotNull();
        assertThat(restored.placa()).isEqualTo("MOTO-RESTORE-TEST");
        assertThat(restored.activo()).isTrue();

        Optional<VehicleEntity> found = vehicleRepository.findById(deleted.getIdVehiculo());
        assertThat(found).isPresent();
        assertThat(found.get().getActivo()).isTrue();
    }

    @Test
    void testRestoreNonExistentSoftDeletedMotoThrows404() {
        assertThatThrownBy(() -> motoService.restoreMoto(9999))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("no encontrada");
    }

    @Test
    void testCanCreateMotoWithNewPlaqueAfterSoftDelete() {
        TipoVehiculoEntity tipoMoto = tipoVehiculoRepository.findByNombreTipoIgnoreCase("MOTOCICLETA")
            .orElseThrow();

        VehicleEntity deleted = vehicleRepository.save(VehicleEntity.builder()
            .placa("MOTO-OLD-PLACA")
            .idMarca(1)
            .idTipoVehiculo(tipoMoto.getId())
            .tipoVehiculo(tipoMoto)
            .activo(false)
            .build());

        VehicleRequest request = new VehicleRequest(
            "MOTO-NEW-PLACA",
            1,
            tipoMoto.getId(),
            100,
            "DISTRICT",
            null,
            null,
            null,
            null,
            null
        );

        VehicleResponse created = motoService.createMoto(request);

        assertThat(created).isNotNull();
        assertThat(created.placa()).isEqualTo("MOTO-NEW-PLACA");
        assertThat(created.activo()).isTrue();
    }
}

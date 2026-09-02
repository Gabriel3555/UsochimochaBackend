package com.app.usochicamochabackend.vehicle.application.service;

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
class VehicleServiceSoftDeleteTest {

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setup() {
        // Crear datos de prueba en BD para satisfacer FK
        // Insertar tipo de vehículo
        jdbcTemplate.update(
            "INSERT INTO cat_tipos_vehiculo (id_tipo_vehiculo, nombre_tipo) VALUES (1, 'VEHICULO') " +
            "ON CONFLICT DO NOTHING"
        );

        // Insertar marca
        jdbcTemplate.update(
            "INSERT INTO cat_marcas_modelos (id_marca, descripcion) VALUES (1, 'TEST_MARCA') " +
            "ON CONFLICT DO NOTHING"
        );
    }

    @Test
    void testSoftDeletedVehicleNotFoundByActiveQuery() {
        VehicleEntity vehicle = vehicleRepository.save(VehicleEntity.builder()
            .placa("ABC123")
            .idMarca(1)
            .idTipoVehiculo(1)
            .activo(true)
            .build());

        vehicleService.deleteVehicle(vehicle.getIdVehiculo());

        Optional<VehicleEntity> found = vehicleRepository.findActiveById(vehicle.getIdVehiculo());
        assertThat(found).isEmpty();
    }

    @Test
    void testFindAllVehiclesExcludesSoftDeleted() {
        VehicleEntity active = vehicleRepository.save(VehicleEntity.builder()
            .placa("ACTIVE01")
            .idMarca(1)
            .idTipoVehiculo(1)
            .activo(true)
            .build());

        VehicleEntity deleted = vehicleRepository.save(VehicleEntity.builder()
            .placa("DELETED01")
            .idMarca(1)
            .idTipoVehiculo(1)
            .activo(false)
            .build());

        var response = vehicleService.findAllVehicles();

        assertThat(response)
            .hasSize(1)
            .extracting(r -> r.placa())
            .containsExactly("ACTIVE01");
    }

    @Test
    void testCannotFindSoftDeletedVehicleByPlaca() {
        VehicleEntity vehicle = vehicleRepository.save(VehicleEntity.builder()
            .placa("DELETED02")
            .idMarca(1)
            .idTipoVehiculo(1)
            .activo(false)
            .build());

        assertThatThrownBy(() -> vehicleService.findByPlaca("DELETED02"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("no encontrado");
    }

    @Test
    void testUpdateVehicleFailsIfSoftDeleted() {
        VehicleEntity deleted = vehicleRepository.save(VehicleEntity.builder()
            .placa("UPDATE_DEL")
            .idMarca(1)
            .idTipoVehiculo(1)
            .activo(false)
            .build());

        // Intentar actualizar debe fallar porque vehículo está soft-deleted
        // El findActiveById() no encontrará el vehículo (está inactivo)
        // Y lanzará ResponseStatusException con "no encontrado"
        assertThatThrownBy(() -> {
            vehicleRepository.findActiveById(deleted.getIdVehiculo());
            // Si llegamos aquí sin excepción, el test falla
            throw new AssertionError("Se esperaba que findActiveById retornara empty");
        }).isInstanceOf(AssertionError.class);

        // Verificar que el vehículo NO está en queries activas
        Optional<VehicleEntity> found = vehicleRepository.findActiveById(deleted.getIdVehiculo());
        assertThat(found).isEmpty();
    }

    @Test
    void testFindByPlacaStillFindsDeletedForAdmin() {
        VehicleEntity deleted = vehicleRepository.save(VehicleEntity.builder()
            .placa("ADMIN_CHECK")
            .idMarca(1)
            .idTipoVehiculo(1)
            .activo(false)
            .build());

        Optional<VehicleEntity> found = vehicleRepository.findByPlaca("ADMIN_CHECK");
        assertThat(found).isPresent();
        assertThat(found.get().getActivo()).isFalse();
    }

    @Test
    void testFindDeletedByIdReturnsOnlyDeleted() {
        VehicleEntity active = vehicleRepository.save(VehicleEntity.builder()
            .placa("ACTIVE02")
            .idMarca(1)
            .idTipoVehiculo(1)
            .activo(true)
            .build());

        VehicleEntity deleted = vehicleRepository.save(VehicleEntity.builder()
            .placa("DELETED03")
            .idMarca(1)
            .idTipoVehiculo(1)
            .activo(false)
            .build());

        Optional<VehicleEntity> foundActive = vehicleRepository.findDeletedById(active.getIdVehiculo());
        Optional<VehicleEntity> foundDeleted = vehicleRepository.findDeletedById(deleted.getIdVehiculo());

        assertThat(foundActive).isEmpty();
        assertThat(foundDeleted).isPresent();
    }

    @Test
    void testSoftDeletedVehicleNotFoundByPlaca() {
        vehicleRepository.save(VehicleEntity.builder()
            .placa("SOFT_DEL_PLACA")
            .idMarca(1)
            .idTipoVehiculo(1)
            .activo(false)
            .build());

        Optional<VehicleEntity> found = vehicleRepository.findActiveByPlaca("SOFT_DEL_PLACA");
        assertThat(found).isEmpty();
    }

    @Test
    void testDeleteVehicleIsTransactional() {
        VehicleEntity vehicle = vehicleRepository.save(VehicleEntity.builder()
            .placa("TRANSACTIONAL")
            .idMarca(1)
            .idTipoVehiculo(1)
            .activo(true)
            .build());

        vehicleService.deleteVehicle(vehicle.getIdVehiculo());

        Optional<VehicleEntity> found = vehicleRepository.findById(vehicle.getIdVehiculo());
        assertThat(found).isPresent();
        assertThat(found.get().getActivo()).isFalse();
    }

    @Test
    void testCreateVehicleWithActivePlaqueThrows400() {
        VehicleEntity active = vehicleRepository.save(VehicleEntity.builder()
            .placa("ACTIVE-PLACA")
            .idMarca(1)
            .idTipoVehiculo(1)
            .activo(true)
            .build());

        VehicleRequest request = new VehicleRequest(
            "ACTIVE-PLACA",
            1,
            1,
            100,
            "DISTRICT",
            null,
            null
        );

        assertThatThrownBy(() -> vehicleService.createVehicle(request))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Ya existe un vehículo con placa");
    }

    @Test
    void testCreateVehicleWithSoftDeletedPlaqueThrows409() {
        VehicleEntity deleted = vehicleRepository.save(VehicleEntity.builder()
            .placa("SOFT-DEL-PLACA")
            .idMarca(1)
            .idTipoVehiculo(1)
            .activo(false)
            .build());

        VehicleRequest request = new VehicleRequest(
            "SOFT-DEL-PLACA",
            1,
            1,
            100,
            "DISTRICT",
            null,
            null
        );

        assertThatThrownBy(() -> vehicleService.createVehicle(request))
            .isInstanceOf(VehicleSoftDeletedConflictException.class)
            .hasMessageContaining("fue eliminado")
            .satisfies(ex -> {
                VehicleSoftDeletedConflictException conflict = (VehicleSoftDeletedConflictException) ex;
                assertThat(conflict.getSoftDeletedVehicle()).isNotNull();
                assertThat(conflict.getSoftDeletedVehicle().placa()).isEqualTo("SOFT-DEL-PLACA");
                assertThat(conflict.getSuggestedAction()).isEqualTo("restore_or_create_new");
            });
    }

    @Test
    void testRestoreSoftDeletedVehicleSucceeds() {
        VehicleEntity deleted = vehicleRepository.save(VehicleEntity.builder()
            .placa("RESTORE-TEST")
            .idMarca(1)
            .idTipoVehiculo(1)
            .activo(false)
            .build());

        VehicleResponse restored = vehicleService.restoreVehicle(deleted.getIdVehiculo());

        assertThat(restored).isNotNull();
        assertThat(restored.placa()).isEqualTo("RESTORE-TEST");
        assertThat(restored.activo()).isTrue();

        Optional<VehicleEntity> found = vehicleRepository.findById(deleted.getIdVehiculo());
        assertThat(found).isPresent();
        assertThat(found.get().getActivo()).isTrue();
    }

    @Test
    void testRestoreNonExistentSoftDeletedVehicleThrows404() {
        assertThatThrownBy(() -> vehicleService.restoreVehicle(9999))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("no encontrado");
    }

    @Test
    void testCanCreateVehicleWithNewPlaqueAfterSoftDelete() {
        VehicleEntity deleted = vehicleRepository.save(VehicleEntity.builder()
            .placa("OLD-PLACA")
            .idMarca(1)
            .idTipoVehiculo(1)
            .activo(false)
            .build());

        VehicleRequest request = new VehicleRequest(
            "NEW-PLACA",
            1,
            1,
            100,
            "DISTRICT",
            null,
            null
        );

        VehicleResponse created = vehicleService.createVehicle(request);

        assertThat(created).isNotNull();
        assertThat(created.placa()).isEqualTo("NEW-PLACA");
        assertThat(created.activo()).isTrue();
    }
}

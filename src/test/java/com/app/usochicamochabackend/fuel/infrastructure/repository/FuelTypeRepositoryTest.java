package com.app.usochicamochabackend.fuel.infrastructure.repository;

import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelTypesEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * H2 (perfil test) no ejecuta las migraciones Flyway (ver application-test.properties),
 * así que el seed de fuel_types se simula manualmente en vez de asumir que ya existe.
 */
@DataJpaTest
@ActiveProfiles("test")
class FuelTypeRepositoryTest {

    @Autowired
    private FuelTypesRepository fuelTypesRepository;

    @Test
    void guardaCuatroTiposYFindAllLosExponeATodos() {
        fuelTypesRepository.save(FuelTypesEntity.builder().codigo("GASOLINA_CORRIENTE").nombre("Gasolina corriente").activo(true).build());
        fuelTypesRepository.save(FuelTypesEntity.builder().codigo("GASOLINA_EXTRA").nombre("Gasolina extra").activo(true).build());
        fuelTypesRepository.save(FuelTypesEntity.builder().codigo("ACPM").nombre("ACPM / Diésel").activo(true).build());
        fuelTypesRepository.save(FuelTypesEntity.builder().codigo("GAS").nombre("Gas natural vehicular").activo(true).unidadMedida("M3").build());

        List<FuelTypesEntity> todos = fuelTypesRepository.findAll();

        assertThat(todos).hasSize(4);
        assertThat(fuelTypesRepository.findByCodigo("GAS")).get().extracting(FuelTypesEntity::getUnidadMedida).isEqualTo("M3");
    }

    @Test
    void findByCodigoResuelveElTipoExistente() {
        fuelTypesRepository.save(FuelTypesEntity.builder().codigo("ACPM").nombre("ACPM / Diésel").activo(true).build());

        Optional<FuelTypesEntity> encontrado = fuelTypesRepository.findByCodigo("ACPM");

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getNombre()).isEqualTo("ACPM / Diésel");
    }

    @Test
    void findByCodigoDevuelveVacioSiNoExiste() {
        assertThat(fuelTypesRepository.findByCodigo("INEXISTENTE")).isEmpty();
    }
}

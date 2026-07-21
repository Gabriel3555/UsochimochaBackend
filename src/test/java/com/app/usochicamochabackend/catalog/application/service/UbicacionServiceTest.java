package com.app.usochicamochabackend.catalog.application.service;

import com.app.usochicamochabackend.catalog.application.dto.CatalogDTO;
import com.app.usochicamochabackend.catalog.infrastructure.entity.UbicacionEntity;
import com.app.usochicamochabackend.catalog.infrastructure.repository.UbicacionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UbicacionServiceTest {

    @Mock
    private UbicacionRepository ubicacionRepository;

    @InjectMocks
    private UbicacionService ubicacionService;

    @Test
    @DisplayName("findAll: retorna lista mapeada")
    void findAll_RetornaLista() {
        UbicacionEntity entity = UbicacionEntity.builder().id(1).nombreUbicacion("Bodega Central").activo(true).build();
        when(ubicacionRepository.findAll()).thenReturn(List.of(entity));

        List<CatalogDTO> result = ubicacionService.findAll();

        assertEquals(1, result.size());
        assertEquals("Bodega Central", result.get(0).name());
    }

    @Test
    @DisplayName("create: nombre válido → guarda y retorna DTO")
    void create_NombreValido_Guarda() {
        UbicacionEntity saved = UbicacionEntity.builder().id(3).nombreUbicacion("Predio Norte").activo(true).build();
        when(ubicacionRepository.existsByNombreUbicacionIgnoreCase(anyString())).thenReturn(false);
        when(ubicacionRepository.save(any())).thenReturn(saved);

        CatalogDTO result = ubicacionService.create(new CatalogDTO(null, "predio norte", true));

        assertEquals(3, result.id());
        assertEquals("Predio Norte", result.name());
    }

    @Test
    @DisplayName("create: nombre en blanco → lanza 400")
    void create_NombreBlanco_LanzaBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> ubicacionService.create(new CatalogDTO(null, "", true)));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("create: nombre duplicado → lanza 409")
    void create_NombreDuplicado_LanzaConflict() {
        when(ubicacionRepository.existsByNombreUbicacionIgnoreCase(anyString())).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> ubicacionService.create(new CatalogDTO(null, "Bodega", true)));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("update: datos válidos → actualiza")
    void update_DatosValidos_Actualiza() {
        UbicacionEntity existing = UbicacionEntity.builder().id(2).nombreUbicacion("Vieja").activo(true).build();
        UbicacionEntity saved = UbicacionEntity.builder().id(2).nombreUbicacion("Nueva").activo(true).build();

        when(ubicacionRepository.findById(2)).thenReturn(Optional.of(existing));
        when(ubicacionRepository.existsByNombreUbicacionIgnoreCaseAndIdNot(anyString(), eq(2))).thenReturn(false);
        when(ubicacionRepository.save(any())).thenReturn(saved);

        CatalogDTO result = ubicacionService.update(2, new CatalogDTO(null, "nueva", true));
        assertEquals("Nueva", result.name());
    }

    @Test
    @DisplayName("update: id no existe → lanza excepción")
    void update_IdNoExiste_LanzaExcepcion() {
        when(ubicacionRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(Exception.class, () -> ubicacionService.update(99, new CatalogDTO(null, "x", true)));
    }

    @Test
    @DisplayName("delete: delega al repositorio")
    void delete_LlamaDeleteById() {
        ubicacionService.delete(5);
        verify(ubicacionRepository).deleteById(5);
    }
}

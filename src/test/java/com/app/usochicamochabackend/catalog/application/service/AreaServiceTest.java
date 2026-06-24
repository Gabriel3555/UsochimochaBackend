package com.app.usochicamochabackend.catalog.application.service;

import com.app.usochicamochabackend.catalog.application.dto.CatalogDTO;
import com.app.usochicamochabackend.catalog.infrastructure.entity.AreaEntity;
import com.app.usochicamochabackend.catalog.infrastructure.repository.AreaRepository;
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
class AreaServiceTest {

    @Mock
    private AreaRepository areaRepository;

    @InjectMocks
    private AreaService areaService;

    @Test
    @DisplayName("findAll: devuelve lista mapeada correctamente")
    void findAll_DebeRetornarLista() {
        AreaEntity entity = AreaEntity.builder().id(1).nombre("Mecánica").activo(true).build();
        when(areaRepository.findAll()).thenReturn(List.of(entity));

        List<CatalogDTO> result = areaService.findAll();

        assertEquals(1, result.size());
        assertEquals("Mecánica", result.get(0).name());
        assertTrue(result.get(0).active());
    }

    @Test
    @DisplayName("create: nombre válido → guarda y retorna DTO con id")
    void create_NombreValido_GuardaArea() {
        AreaEntity saved = AreaEntity.builder().id(5).nombre("Sistemas").activo(true).build();
        when(areaRepository.existsByNombre(anyString())).thenReturn(false);
        when(areaRepository.save(any())).thenReturn(saved);

        CatalogDTO result = areaService.create(new CatalogDTO(null, "sistemas", true));

        assertEquals(5, result.id());
        assertEquals("Sistemas", result.name());
        verify(areaRepository).save(any(AreaEntity.class));
    }

    @Test
    @DisplayName("create: nombre en blanco → lanza 400")
    void create_NombreBlanco_LanzaBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> areaService.create(new CatalogDTO(null, "   ", true)));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("create: nombre duplicado → lanza 409")
    void create_NombreDuplicado_LanzaConflict() {
        when(areaRepository.existsByNombre(anyString())).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> areaService.create(new CatalogDTO(null, "Sistemas", true)));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("update: id existente y nombre válido → actualiza")
    void update_DatosValidos_Actualiza() {
        AreaEntity existing = AreaEntity.builder().id(1).nombre("Vieja").activo(true).build();
        AreaEntity saved = AreaEntity.builder().id(1).nombre("Nueva").activo(false).build();

        when(areaRepository.findById(1)).thenReturn(Optional.of(existing));
        when(areaRepository.existsByNombreAndIdNot(anyString(), eq(1))).thenReturn(false);
        when(areaRepository.save(any())).thenReturn(saved);

        CatalogDTO result = areaService.update(1, new CatalogDTO(null, "nueva", false));

        assertEquals("Nueva", result.name());
        assertFalse(result.active());
    }

    @Test
    @DisplayName("update: id no existente → lanza NoSuchElement")
    void update_IdNoExiste_LanzaExcepcion() {
        when(areaRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(Exception.class, () -> areaService.update(99, new CatalogDTO(null, "Test", true)));
    }

    @Test
    @DisplayName("delete: delega al repositorio")
    void delete_LlamaDeleteById() {
        areaService.delete(3);
        verify(areaRepository).deleteById(3);
    }
}

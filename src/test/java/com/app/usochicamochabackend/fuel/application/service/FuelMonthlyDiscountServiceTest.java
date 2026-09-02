package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.actions.application.port.SaveActionUseCase;
import com.app.usochicamochabackend.exception.ResourceNotFoundException;
import com.app.usochicamochabackend.fuel.application.dto.FuelMonthlyDiscountRequest;
import com.app.usochicamochabackend.fuel.application.dto.FuelMonthlyDiscountResponse;
import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelMonthlyDiscountEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelMonthlyDiscountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FuelMonthlyDiscountServiceTest {

    @Mock private FuelMonthlyDiscountRepository fuelMonthlyDiscountRepository;
    @Mock private SaveActionUseCase saveActionUseCase;

    @InjectMocks
    private FuelMonthlyDiscountService fuelMonthlyDiscountService;

    @Test
    void registrar_ConDatosValidos_Guarda() {
        when(fuelMonthlyDiscountRepository.save(any())).thenAnswer(invocation -> {
            FuelMonthlyDiscountEntity e = invocation.getArgument(0);
            e.setId(1L);
            return e;
        });

        FuelMonthlyDiscountRequest request = new FuelMonthlyDiscountRequest(
                LocalDate.of(2026, 7, 16), LocalDate.of(2026, 8, 15), new BigDecimal("80000"));
        FuelMonthlyDiscountResponse response = fuelMonthlyDiscountService.registrar(request, 9L);

        assertEquals(1L, response.id());
        assertEquals(LocalDate.of(2026, 7, 16), response.fechaInicio());
        assertEquals(LocalDate.of(2026, 8, 15), response.fechaFin());
        assertEquals(0, new BigDecimal("80000").compareTo(response.monto()));
        verify(saveActionUseCase).save(anyString());
    }

    @Test
    void registrar_ConFechaFinAnteriorAFechaInicio_Lanza400() {
        FuelMonthlyDiscountRequest request = new FuelMonthlyDiscountRequest(
                LocalDate.of(2026, 8, 15), LocalDate.of(2026, 7, 16), new BigDecimal("80000"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> fuelMonthlyDiscountService.registrar(request, 9L));

        assertEquals(400, ex.getStatusCode().value());
        verify(fuelMonthlyDiscountRepository, never()).save(any());
    }

    @Test
    void registrar_ConMontoCeroOMenos_Lanza400() {
        FuelMonthlyDiscountRequest request = new FuelMonthlyDiscountRequest(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), BigDecimal.ZERO);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> fuelMonthlyDiscountService.registrar(request, 9L));

        assertEquals(400, ex.getStatusCode().value());
        verify(fuelMonthlyDiscountRepository, never()).save(any());
    }

    @Test
    void registrar_ConCamposNulos_Lanza400() {
        FuelMonthlyDiscountRequest request = new FuelMonthlyDiscountRequest(null, null, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> fuelMonthlyDiscountService.registrar(request, 9L));

        assertEquals(400, ex.getStatusCode().value());
        verify(fuelMonthlyDiscountRepository, never()).save(any());
    }

    @Test
    void listar_DevuelveSoloLosActivosOrdenadosPorFechaInicioDesc() {
        FuelMonthlyDiscountEntity entity = FuelMonthlyDiscountEntity.builder()
                .id(2L).fechaInicio(LocalDate.of(2026, 7, 1)).fechaFin(LocalDate.of(2026, 7, 31))
                .monto(new BigDecimal("50000")).status(true).build();
        when(fuelMonthlyDiscountRepository.findByStatusTrueOrderByFechaInicioDesc()).thenReturn(List.of(entity));

        List<FuelMonthlyDiscountResponse> resultado = fuelMonthlyDiscountService.listar();

        assertEquals(1, resultado.size());
        assertEquals(2L, resultado.get(0).id());
    }

    @Test
    void eliminar_MarcaStatusFalse() {
        FuelMonthlyDiscountEntity entity = FuelMonthlyDiscountEntity.builder().id(3L).status(true).build();
        when(fuelMonthlyDiscountRepository.findById(3L)).thenReturn(Optional.of(entity));
        when(fuelMonthlyDiscountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        fuelMonthlyDiscountService.eliminar(3L);

        assertFalse(entity.getStatus());
        verify(fuelMonthlyDiscountRepository).save(entity);
        verify(saveActionUseCase).save(anyString());
    }

    @Test
    void eliminar_ConIdInexistente_Lanza404() {
        when(fuelMonthlyDiscountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> fuelMonthlyDiscountService.eliminar(99L));
        verify(fuelMonthlyDiscountRepository, never()).save(any());
    }
}

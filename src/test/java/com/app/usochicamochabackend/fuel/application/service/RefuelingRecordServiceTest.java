package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.actions.application.port.SaveActionUseCase;
import com.app.usochicamochabackend.fuel.application.dto.RefuelingRecordRequest;
import com.app.usochicamochabackend.fuel.application.dto.RefuelingRecordResponse;
import com.app.usochicamochabackend.fuel.application.port.AdjustFuelInventoryUseCase;
import com.app.usochicamochabackend.fuel.infrastructure.entity.RefuelingRecordsEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.RefuelingRecordsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefuelingRecordServiceTest {

    @Mock
    private RefuelingRecordsRepository refuelingRecordsRepository;

    @Mock
    private AdjustFuelInventoryUseCase adjustFuelInventoryUseCase;

    @Mock
    private FuelDocumentStorageService fuelDocumentStorageService;

    @Mock
    private SaveActionUseCase saveActionUseCase;

    private RefuelingRecordService refuelingRecordService;

    private final MultipartFile factura = new MockMultipartFile("factura", "f.pdf", "application/pdf", new byte[]{1, 2, 3});

    @BeforeEach
    void setUp() {
        refuelingRecordService = new RefuelingRecordService(
                refuelingRecordsRepository, adjustFuelInventoryUseCase, fuelDocumentStorageService, saveActionUseCase);
        ReflectionTestUtils.setField(refuelingRecordService, "tolerancia", new BigDecimal("0.01"));
    }

    private void stubSaveAsignandoId() {
        when(refuelingRecordsRepository.save(any())).thenAnswer(invocation -> {
            RefuelingRecordsEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(1L);
            }
            return entity;
        });
    }

    @Test
    void tanqueoAlmacenConStockSuficiente_DescuentaInventarioYPersiste() {
        stubSaveAsignandoId();
        RefuelingRecordRequest request = new RefuelingRecordRequest(
                null, 10L, "ALMACEN", "DISTRITO", 1L, new BigDecimal("30"), new BigDecimal("500"),
                false, null, null, null, "Bodega central");

        RefuelingRecordResponse response = refuelingRecordService.registrar(request, null, 7L);

        verify(adjustFuelInventoryUseCase).decrement("DISTRITO", 1L, new BigDecimal("30"));
        verify(refuelingRecordsRepository).save(any());
        assertNull(response.totalCalculado());
        verify(saveActionUseCase).save(anyString());
    }

    @Test
    void tanqueoAlmacenConStockInsuficiente_Lanza409YNoPersiste() {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Stock insuficiente"))
                .when(adjustFuelInventoryUseCase).decrement("DISTRITO", 1L, new BigDecimal("30"));

        RefuelingRecordRequest request = new RefuelingRecordRequest(
                null, 10L, "ALMACEN", "DISTRITO", 1L, new BigDecimal("30"), new BigDecimal("500"),
                false, null, null, null, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> refuelingRecordService.registrar(request, null, 7L));

        assertEquals(409, ex.getStatusCode().value());
        verify(refuelingRecordsRepository, never()).save(any());
    }

    @Test
    void vehicleIdYMachineIdAmbosPresentes_Lanza400AntesDeTocarBD() {
        RefuelingRecordRequest request = new RefuelingRecordRequest(
                3, 10L, "ALMACEN", "DISTRITO", 1L, new BigDecimal("30"), new BigDecimal("500"),
                false, null, null, null, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> refuelingRecordService.registrar(request, null, 7L));

        assertEquals(400, ex.getStatusCode().value());
        verifyNoInteractions(adjustFuelInventoryUseCase, refuelingRecordsRepository);
    }

    @Test
    void vehicleIdYMachineIdAmbosAusentes_Lanza400AntesDeTocarBD() {
        RefuelingRecordRequest request = new RefuelingRecordRequest(
                null, null, "ALMACEN", "DISTRITO", 1L, new BigDecimal("30"), new BigDecimal("500"),
                false, null, null, null, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> refuelingRecordService.registrar(request, null, 7L));

        assertEquals(400, ex.getStatusCode().value());
        verifyNoInteractions(adjustFuelInventoryUseCase, refuelingRecordsRepository);
    }

    @Test
    void tanqueoBombaSinFactura_Lanza400() {
        RefuelingRecordRequest request = new RefuelingRecordRequest(
                3, null, "BOMBA", "DISTRITO", 1L, new BigDecimal("30"), new BigDecimal("500"),
                false, new BigDecimal("10000"), null, new BigDecimal("300000"), null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> refuelingRecordService.registrar(request, null, 7L));

        assertEquals(400, ex.getStatusCode().value());
        verify(refuelingRecordsRepository, never()).save(any());
    }

    @Test
    void tanqueoBombaConDiscrepancia_MarcaDiscrepanciaTrueYGuardaFactura() throws Exception {
        stubSaveAsignandoId();
        when(fuelDocumentStorageService.store(any(), anyString(), anyLong()))
                .thenReturn("/uploads/documents/fuel/refueling/1/current.pdf");

        // totalCalculado = 30 * 10000 = 300000; totalIngresado con 10% de diferencia
        RefuelingRecordRequest request = new RefuelingRecordRequest(
                3, null, "BOMBA", "DISTRITO", 1L, new BigDecimal("30"), new BigDecimal("500"),
                false, new BigDecimal("10000"), null, new BigDecimal("270000"), null);

        RefuelingRecordResponse response = refuelingRecordService.registrar(request, factura, 7L);

        assertTrue(response.discrepanciaValor());
        assertEquals("/uploads/documents/fuel/refueling/1/current.pdf", response.urlFactura());
        verifyNoInteractions(adjustFuelInventoryUseCase);
    }
}

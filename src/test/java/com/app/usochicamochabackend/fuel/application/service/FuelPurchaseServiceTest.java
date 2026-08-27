package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.actions.application.port.SaveActionUseCase;
import com.app.usochicamochabackend.fuel.application.dto.FuelPurchaseRequest;
import com.app.usochicamochabackend.fuel.application.dto.FuelPurchaseResponse;
import com.app.usochicamochabackend.fuel.application.port.AdjustFuelInventoryUseCase;
import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelPurchaseEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelPurchaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
class FuelPurchaseServiceTest {

    @Mock
    private FuelPurchaseRepository fuelPurchaseRepository;

    @Mock
    private AdjustFuelInventoryUseCase adjustFuelInventoryUseCase;

    @Mock
    private FuelDocumentStorageService fuelDocumentStorageService;

    @Mock
    private SaveActionUseCase saveActionUseCase;

    private FuelPurchaseService fuelPurchaseService;

    private final MultipartFile factura = new MockMultipartFile("factura", "f.pdf", "application/pdf", new byte[]{1, 2, 3});

    @BeforeEach
    void setUp() throws Exception {
        fuelPurchaseService = new FuelPurchaseService(
                fuelPurchaseRepository, adjustFuelInventoryUseCase, fuelDocumentStorageService, saveActionUseCase);
        ReflectionTestUtils.setField(fuelPurchaseService, "tolerancia", new BigDecimal("0.01"));
    }

    private void stubGuardadoYFactura() throws Exception {
        when(fuelPurchaseRepository.save(any())).thenAnswer(invocation -> {
            FuelPurchaseEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(1L);
            }
            return entity;
        });
        when(fuelDocumentStorageService.store(any(), anyString(), anyLong()))
                .thenReturn("/uploads/documents/fuel/purchases/1/current.pdf");
    }

    @Test
    void registrarCompraSinDiscrepancia_MarcaDiscrepanciaFalseYSumaInventario() throws Exception {
        stubGuardadoYFactura();
        FuelPurchaseRequest request = new FuelPurchaseRequest(
                "DISTRITO", 1L, new BigDecimal("100"), new BigDecimal("10000"), null, new BigDecimal("1000000"));

        FuelPurchaseResponse response = fuelPurchaseService.registrar(request, factura, 5L);

        assertEquals(0, new BigDecimal("1000000").compareTo(response.totalCalculado()));
        assertFalse(response.discrepanciaValor());
        verify(adjustFuelInventoryUseCase).increment("DISTRITO", 1L, new BigDecimal("100"));
        verify(saveActionUseCase).save(anyString());
    }

    @Test
    void registrarCompraConDiscrepanciaMayorAlUnoPorciento_MarcaDiscrepanciaTrue() throws Exception {
        stubGuardadoYFactura();
        // totalCalculado = 1_000_000; totalIngresado con 5% de diferencia
        FuelPurchaseRequest request = new FuelPurchaseRequest(
                "DISTRITO", 1L, new BigDecimal("100"), new BigDecimal("10000"), null, new BigDecimal("950000"));

        FuelPurchaseResponse response = fuelPurchaseService.registrar(request, factura, 5L);

        assertTrue(response.discrepanciaValor());
    }

    @Test
    void registrarSinFactura_LanzaBadRequestYNoTocaInventario() {
        FuelPurchaseRequest request = new FuelPurchaseRequest(
                "DISTRITO", 1L, new BigDecimal("100"), new BigDecimal("10000"), null, new BigDecimal("1000000"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> fuelPurchaseService.registrar(request, null, 5L));

        assertEquals(400, ex.getStatusCode().value());
        verify(adjustFuelInventoryUseCase, never()).increment(any(), any(), any());
        verify(fuelPurchaseRepository, never()).save(any());
    }

    @Test
    void registrarSinCamposObligatorios_LanzaBadRequest() {
        FuelPurchaseRequest request = new FuelPurchaseRequest(
                null, 1L, new BigDecimal("100"), new BigDecimal("10000"), null, new BigDecimal("1000000"));

        assertThrows(ResponseStatusException.class, () -> fuelPurchaseService.registrar(request, factura, 5L));
        verify(fuelPurchaseRepository, never()).save(any());
    }
}

package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.actions.application.port.SaveActionUseCase;
import com.app.usochicamochabackend.fuel.application.dto.RefuelingRecordRequest;
import com.app.usochicamochabackend.fuel.application.dto.RefuelingRecordResponse;
import com.app.usochicamochabackend.fuel.application.port.AdjustFuelInventoryUseCase;
import com.app.usochicamochabackend.fuel.infrastructure.entity.RefuelingRecordsEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.RefuelingRecordsRepository;
import com.app.usochicamochabackend.machine.infrastructure.entity.MachineEntity;
import com.app.usochicamochabackend.machine.infrastructure.repository.MachineRepository;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.VehicleEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

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

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private MachineRepository machineRepository;

    @Mock
    private AssetFuelCapacityService assetFuelCapacityService;

    private RefuelingRecordService refuelingRecordService;

    private final MultipartFile factura = new MockMultipartFile("factura", "f.pdf", "application/pdf", new byte[]{1, 2, 3});

    @BeforeEach
    void setUp() {
        refuelingRecordService = new RefuelingRecordService(
                refuelingRecordsRepository, adjustFuelInventoryUseCase, fuelDocumentStorageService, saveActionUseCase,
                vehicleRepository, machineRepository, assetFuelCapacityService);
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
    void tanqueoVehiculoConKmMayorAlActual_ActualizaKilometrajeDelVehiculo() {
        // Mismo patrón que VehicleOilChangeService: el km del tanqueo pasa a ser el
        // "kilometraje actual" del vehículo (Inventario, alertas, etc.) solo si es
        // mayor al que ya tenía — para que quede consistente en todo el sistema.
        stubSaveAsignandoId();
        VehicleEntity vehicle = VehicleEntity.builder().idVehiculo(3).kilometrajeActual(400).build();
        when(vehicleRepository.findById(3)).thenReturn(java.util.Optional.of(vehicle));
        RefuelingRecordRequest request = new RefuelingRecordRequest(
                3, null, "ALMACEN", "DISTRITO", 1L, new BigDecimal("30"), new BigDecimal("500"),
                false, null, null, null, null);

        refuelingRecordService.registrar(request, null, 7L);

        verify(vehicleRepository).save(argThat(v -> v.getKilometrajeActual() == 500));
    }

    @Test
    void tanqueoVehiculoConKmMenorOIgualAlActual_NoActualizaKilometraje() {
        stubSaveAsignandoId();
        VehicleEntity vehicle = VehicleEntity.builder().idVehiculo(3).kilometrajeActual(600).build();
        when(vehicleRepository.findById(3)).thenReturn(java.util.Optional.of(vehicle));
        RefuelingRecordRequest request = new RefuelingRecordRequest(
                3, null, "ALMACEN", "DISTRITO", 1L, new BigDecimal("30"), new BigDecimal("500"),
                false, null, null, null, null);

        refuelingRecordService.registrar(request, null, 7L);

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void tanqueoMaquinaConHorometroMayorAlActual_ActualizaHorometroDeLaMaquina() {
        stubSaveAsignandoId();
        MachineEntity machine = MachineEntity.builder().id(10L).horometroActual(100).build();
        when(machineRepository.findById(10L)).thenReturn(java.util.Optional.of(machine));
        RefuelingRecordRequest request = new RefuelingRecordRequest(
                null, 10L, "ALMACEN", "DISTRITO", 1L, new BigDecimal("30"), new BigDecimal("150"),
                false, null, null, null, null);

        refuelingRecordService.registrar(request, null, 7L);

        verify(machineRepository).save(argThat(m -> m.getHorometroActual() == 150));
    }

    @Test
    void tanqueoMaquinaConHorometroMenorOIgualAlActual_NoActualiza() {
        stubSaveAsignandoId();
        MachineEntity machine = MachineEntity.builder().id(10L).horometroActual(200).build();
        when(machineRepository.findById(10L)).thenReturn(java.util.Optional.of(machine));
        RefuelingRecordRequest request = new RefuelingRecordRequest(
                null, 10L, "ALMACEN", "DISTRITO", 1L, new BigDecimal("30"), new BigDecimal("150"),
                false, null, null, null, null);

        refuelingRecordService.registrar(request, null, 7L);

        verify(machineRepository, never()).save(any());
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

    @Test
    void tanqueoBomba_PrimerGuardadoNoManda_UrlFacturaNula() throws Exception {
        // Bug real contra Postgres (no lo detecta H2 en tests, que no aplica el CHECK de
        // la migración V20): `lugar <> 'BOMBA' OR url_factura IS NOT NULL` se evalúa en el
        // INSERT mismo, antes de que exista el id necesario para subir el archivo real —
        // el primer guardado necesita un placeholder no nulo, igual que ya hace
        // FuelPurchaseService con "pendiente".
        // OJO: no usar ArgumentCaptor aquí — reutiliza la MISMA referencia mutable entre
        // ambos save(), así que capturar el objeto y leerlo después siempre ve el valor
        // final (falso negativo). Hay que tomar el snapshot del valor en el momento exacto
        // de cada llamada, dentro del propio stub.
        java.util.List<String> urlFacturaEnCadaGuardado = new java.util.ArrayList<>();
        when(refuelingRecordsRepository.save(any())).thenAnswer(invocation -> {
            RefuelingRecordsEntity entity = invocation.getArgument(0);
            urlFacturaEnCadaGuardado.add(entity.getUrlFactura());
            if (entity.getId() == null) {
                entity.setId(1L);
            }
            return entity;
        });
        when(fuelDocumentStorageService.store(any(), anyString(), anyLong()))
                .thenReturn("/uploads/documents/fuel/refueling/1/current.pdf");

        RefuelingRecordRequest request = new RefuelingRecordRequest(
                3, null, "BOMBA", "DISTRITO", 1L, new BigDecimal("30"), new BigDecimal("500"),
                false, new BigDecimal("10000"), null, new BigDecimal("300000"), null);

        refuelingRecordService.registrar(request, factura, 7L);

        assertEquals(2, urlFacturaEnCadaGuardado.size());
        assertNotNull(urlFacturaEnCadaGuardado.get(0));
    }

    // ---- actualizar() ----

    @Test
    void actualizarTanqueoBomba_RecalculaTotalYDiscrepancia() {
        RefuelingRecordsEntity existente = RefuelingRecordsEntity.builder()
                .id(1L).vehicleId(3).lugar("BOMBA").areaCosto("DISTRITO").fuelTypeId(1L)
                .cantidadGalones(new BigDecimal("30")).horometroKm(new BigDecimal("500"))
                .precioUnitario(new BigDecimal("9000")).totalIngresado(new BigDecimal("270000"))
                .totalCalculado(new BigDecimal("270000")).discrepanciaValor(false)
                .urlFactura("/uploads/documents/fuel/refueling/1/old.pdf").status(true)
                .build();
        when(refuelingRecordsRepository.findByIdAndStatus(1L, true)).thenReturn(Optional.of(existente));
        stubSaveAsignandoId();

        // totalCalculado = 30 * 10000 = 300000; totalIngresado 270000 -> discrepancia
        RefuelingRecordRequest request = new RefuelingRecordRequest(
                3, null, "BOMBA", "DISTRITO", 1L, new BigDecimal("30"), new BigDecimal("500"),
                false, new BigDecimal("10000"), null, new BigDecimal("270000"), null);

        RefuelingRecordResponse response = refuelingRecordService.actualizar(1L, request, null);

        assertEquals(0, new BigDecimal("300000").compareTo(response.totalCalculado()));
        assertTrue(response.discrepanciaValor());
        verifyNoInteractions(adjustFuelInventoryUseCase);
        verify(saveActionUseCase).save(anyString());
    }

    @Test
    void actualizarTanqueoAlmacenCambiandoActivoYCantidad_RevierteYAplicaInventarioCorrecto() {
        RefuelingRecordsEntity existente = RefuelingRecordsEntity.builder()
                .id(2L).vehicleId(3).lugar("ALMACEN").areaCosto("DISTRITO").fuelTypeId(1L)
                .cantidadGalones(new BigDecimal("30")).horometroKm(new BigDecimal("500")).status(true)
                .build();
        when(refuelingRecordsRepository.findByIdAndStatus(2L, true)).thenReturn(Optional.of(existente));
        stubSaveAsignandoId();

        RefuelingRecordRequest request = new RefuelingRecordRequest(
                null, 10L, "ALMACEN", "ASOCIACION", 2L, new BigDecimal("50"), new BigDecimal("150"),
                false, null, null, null, "Bodega nueva");

        refuelingRecordService.actualizar(2L, request, null);

        InOrder orden = inOrder(adjustFuelInventoryUseCase);
        orden.verify(adjustFuelInventoryUseCase).increment("DISTRITO", 1L, new BigDecimal("30"));
        orden.verify(adjustFuelInventoryUseCase).decrement("ASOCIACION", 2L, new BigDecimal("50"));
    }

    @Test
    void actualizarTanqueoDeAlmacenABomba_ExigeFacturaSiNoTeniaUnaReal() {
        RefuelingRecordsEntity existente = RefuelingRecordsEntity.builder()
                .id(3L).vehicleId(3).lugar("ALMACEN").areaCosto("DISTRITO").fuelTypeId(1L)
                .cantidadGalones(new BigDecimal("30")).horometroKm(new BigDecimal("500"))
                .urlFactura(null).status(true)
                .build();
        when(refuelingRecordsRepository.findByIdAndStatus(3L, true)).thenReturn(Optional.of(existente));

        RefuelingRecordRequest request = new RefuelingRecordRequest(
                3, null, "BOMBA", "DISTRITO", 1L, new BigDecimal("30"), new BigDecimal("500"),
                false, new BigDecimal("10000"), null, new BigDecimal("300000"), null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> refuelingRecordService.actualizar(3L, request, null));

        assertEquals(400, ex.getStatusCode().value());
        verify(refuelingRecordsRepository, never()).save(any());
    }

    @Test
    void actualizarTanqueoDeAlmacenABomba_ConFacturaNueva_GuardaUrlFactura() throws Exception {
        RefuelingRecordsEntity existente = RefuelingRecordsEntity.builder()
                .id(3L).vehicleId(3).lugar("ALMACEN").areaCosto("DISTRITO").fuelTypeId(1L)
                .cantidadGalones(new BigDecimal("30")).horometroKm(new BigDecimal("500"))
                .urlFactura(null).status(true)
                .build();
        when(refuelingRecordsRepository.findByIdAndStatus(3L, true)).thenReturn(Optional.of(existente));
        stubSaveAsignandoId();
        when(fuelDocumentStorageService.store(any(), anyString(), anyLong()))
                .thenReturn("/uploads/documents/fuel/refueling/3/current.pdf");

        RefuelingRecordRequest request = new RefuelingRecordRequest(
                3, null, "BOMBA", "DISTRITO", 1L, new BigDecimal("30"), new BigDecimal("500"),
                false, new BigDecimal("10000"), null, new BigDecimal("300000"), null);

        RefuelingRecordResponse response = refuelingRecordService.actualizar(3L, request, factura);

        assertEquals("/uploads/documents/fuel/refueling/3/current.pdf", response.urlFactura());
        // Al salir de ALMACEN se revierte el inventario viejo; al entrar a BOMBA no se descuenta nada.
        verify(adjustFuelInventoryUseCase).increment("DISTRITO", 1L, new BigDecimal("30"));
        verify(adjustFuelInventoryUseCase, never()).decrement(any(), any(), any());
    }

    @Test
    void actualizarTanqueoYaBombaSinFacturaNueva_MantieneLaFacturaExistente() {
        RefuelingRecordsEntity existente = RefuelingRecordsEntity.builder()
                .id(4L).vehicleId(3).lugar("BOMBA").areaCosto("DISTRITO").fuelTypeId(1L)
                .cantidadGalones(new BigDecimal("30")).horometroKm(new BigDecimal("500"))
                .precioUnitario(new BigDecimal("9000")).totalIngresado(new BigDecimal("270000"))
                .urlFactura("/uploads/documents/fuel/refueling/4/existing.pdf").status(true)
                .build();
        when(refuelingRecordsRepository.findByIdAndStatus(4L, true)).thenReturn(Optional.of(existente));
        stubSaveAsignandoId();

        RefuelingRecordRequest request = new RefuelingRecordRequest(
                3, null, "BOMBA", "DISTRITO", 1L, new BigDecimal("30"), new BigDecimal("500"),
                false, new BigDecimal("9000"), null, new BigDecimal("270000"), null);

        RefuelingRecordResponse response = refuelingRecordService.actualizar(4L, request, null);

        assertEquals("/uploads/documents/fuel/refueling/4/existing.pdf", response.urlFactura());
        verifyNoInteractions(fuelDocumentStorageService);
    }

    @Test
    void actualizarNuevoStockInsuficiente_Lanza409() {
        RefuelingRecordsEntity existente = RefuelingRecordsEntity.builder()
                .id(2L).vehicleId(3).lugar("ALMACEN").areaCosto("DISTRITO").fuelTypeId(1L)
                .cantidadGalones(new BigDecimal("30")).horometroKm(new BigDecimal("500")).status(true)
                .build();
        when(refuelingRecordsRepository.findByIdAndStatus(2L, true)).thenReturn(Optional.of(existente));
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Stock insuficiente"))
                .when(adjustFuelInventoryUseCase).decrement("ASOCIACION", 2L, new BigDecimal("50"));

        RefuelingRecordRequest request = new RefuelingRecordRequest(
                null, 10L, "ALMACEN", "ASOCIACION", 2L, new BigDecimal("50"), new BigDecimal("150"),
                false, null, null, null, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> refuelingRecordService.actualizar(2L, request, null));

        assertEquals(409, ex.getStatusCode().value());
        verify(refuelingRecordsRepository, never()).save(any());
    }

    @Test
    void actualizarTanqueoInexistenteOInactivo_Lanza404() {
        when(refuelingRecordsRepository.findByIdAndStatus(99L, true)).thenReturn(Optional.empty());

        RefuelingRecordRequest request = new RefuelingRecordRequest(
                3, null, "BOMBA", "DISTRITO", 1L, new BigDecimal("30"), new BigDecimal("500"),
                false, new BigDecimal("9000"), null, new BigDecimal("270000"), null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> refuelingRecordService.actualizar(99L, request, null));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void actualizarTanqueoConHorometroMayor_SincronizaLecturaDelActivo() {
        RefuelingRecordsEntity existente = RefuelingRecordsEntity.builder()
                .id(5L).vehicleId(3).lugar("ALMACEN").areaCosto("DISTRITO").fuelTypeId(1L)
                .cantidadGalones(new BigDecimal("30")).horometroKm(new BigDecimal("500")).status(true)
                .build();
        when(refuelingRecordsRepository.findByIdAndStatus(5L, true)).thenReturn(Optional.of(existente));
        stubSaveAsignandoId();
        VehicleEntity vehicle = VehicleEntity.builder().idVehiculo(3).kilometrajeActual(500).build();
        when(vehicleRepository.findById(3)).thenReturn(Optional.of(vehicle));

        RefuelingRecordRequest request = new RefuelingRecordRequest(
                3, null, "ALMACEN", "DISTRITO", 1L, new BigDecimal("30"), new BigDecimal("650"),
                false, null, null, null, null);

        refuelingRecordService.actualizar(5L, request, null);

        verify(vehicleRepository).save(argThat(v -> v.getKilometrajeActual() == 650));
    }

    // ---- eliminar() ----

    @Test
    void eliminarTanqueoBomba_SoloMarcaInactivoSinTocarInventario() {
        RefuelingRecordsEntity existente = RefuelingRecordsEntity.builder()
                .id(6L).vehicleId(3).lugar("BOMBA").areaCosto("DISTRITO").fuelTypeId(1L)
                .cantidadGalones(new BigDecimal("30")).status(true)
                .build();
        when(refuelingRecordsRepository.findByIdAndStatus(6L, true)).thenReturn(Optional.of(existente));

        refuelingRecordService.eliminar(6L);

        verify(refuelingRecordsRepository).save(argThat(e -> Boolean.FALSE.equals(e.getStatus())));
        verifyNoInteractions(adjustFuelInventoryUseCase);
        verify(saveActionUseCase).save(anyString());
    }

    @Test
    void eliminarTanqueoAlmacen_RevierteInventarioConIncrement() {
        RefuelingRecordsEntity existente = RefuelingRecordsEntity.builder()
                .id(7L).machineId(10L).lugar("ALMACEN").areaCosto("DISTRITO").fuelTypeId(1L)
                .cantidadGalones(new BigDecimal("30")).status(true)
                .build();
        when(refuelingRecordsRepository.findByIdAndStatus(7L, true)).thenReturn(Optional.of(existente));

        refuelingRecordService.eliminar(7L);

        verify(adjustFuelInventoryUseCase).increment("DISTRITO", 1L, new BigDecimal("30"));
        verify(refuelingRecordsRepository).save(argThat(e -> Boolean.FALSE.equals(e.getStatus())));
    }

    @Test
    void eliminarTanqueoInexistenteOYaInactivo_Lanza404() {
        when(refuelingRecordsRepository.findByIdAndStatus(99L, true)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> refuelingRecordService.eliminar(99L));

        assertEquals(404, ex.getStatusCode().value());
        verify(refuelingRecordsRepository, never()).save(any());
    }

    @Test
    void registrarConCantidadQueExcedeLaCapacidadDelTanque_MarcaCapacidadExcedidaEnLaRespuesta() {
        stubSaveAsignandoId();
        when(assetFuelCapacityService.excedeCapacidad(3, null, new BigDecimal("30"))).thenReturn(true);
        RefuelingRecordRequest request = new RefuelingRecordRequest(
                3, null, "ALMACEN", "DISTRITO", 1L, new BigDecimal("30"), new BigDecimal("500"),
                false, null, null, null, null);

        RefuelingRecordResponse response = refuelingRecordService.registrar(request, null, 7L);

        assertTrue(response.capacidadExcedida());
    }
}

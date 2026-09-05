package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.fuel.infrastructure.entity.AssetFuelConfigEntity;
import com.app.usochicamochabackend.fuel.infrastructure.entity.RefuelingRecordsEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.AssetFuelConfigRepository;
import com.app.usochicamochabackend.fuel.infrastructure.repository.RefuelingRecordsRepository;
import com.app.usochicamochabackend.machine.infrastructure.entity.MachineEntity;
import com.app.usochicamochabackend.machine.infrastructure.repository.MachineRepository;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.VehicleEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FuelMonthlyPerformanceExcelExportServiceTest {

    @Mock
    private RefuelingRecordsRepository refuelingRecordsRepository;
    @Mock
    private AssetFuelConfigRepository assetFuelConfigRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private MachineRepository machineRepository;

    private FuelMonthlyPerformanceExcelExportService service;

    @BeforeEach
    void setUp() {
        service = new FuelMonthlyPerformanceExcelExportService(
                refuelingRecordsRepository, assetFuelConfigRepository, vehicleRepository, machineRepository);
    }

    private RefuelingRecordsEntity tanqueo(Long machineId, LocalDateTime fecha, BigDecimal horometro, BigDecimal galones) {
        return RefuelingRecordsEntity.builder()
                .id(1L).machineId(machineId).lugar("ALMACEN").areaCosto("DISTRITO").fuelTypeId(1L)
                .cantidadGalones(galones).horometroKm(horometro).esFull(false).status(true)
                .fechaRegistro(Timestamp.valueOf(fecha))
                .build();
    }

    private Workbook leer(byte[] excel) throws IOException {
        return new XSSFWorkbook(new ByteArrayInputStream(excel));
    }

    @Test
    void generaUnaHojaPorCadaMesDelRango_InclusoSinTanqueosEseMes() throws IOException {
        when(assetFuelConfigRepository.findAll()).thenReturn(List.of());
        when(refuelingRecordsRepository.findByFechaRegistroBetweenAndStatusTrue(any(), any())).thenReturn(List.of());

        byte[] excel = service.exportarMensual(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 15));

        Workbook workbook = leer(excel);
        assertEquals(3, workbook.getNumberOfSheets());
        assertNotNull(workbook.getSheet("2026-07"));
        assertNotNull(workbook.getSheet("2026-08"));
        assertNotNull(workbook.getSheet("2026-09"));
    }

    @Test
    void calculaPrimeroUltimoConsumoYRecorridoDelMesParaUnActivo() throws IOException {
        AssetFuelConfigEntity config = AssetFuelConfigEntity.builder()
                .id(1L).machineId(10L).consumoEstandar(new BigDecimal("1.5")).unidadConsumo("HORA_POR_GALON").build();
        when(assetFuelConfigRepository.findAll()).thenReturn(List.of(config));
        when(machineRepository.findById(10L)).thenReturn(java.util.Optional.of(
                MachineEntity.builder().id(10L).name("Retro Cat 02").build()));

        List<RefuelingRecordsEntity> tanqueos = List.of(
                tanqueo(10L, LocalDateTime.of(2026, 7, 5, 8, 0), new BigDecimal("100"), new BigDecimal("10")),
                tanqueo(10L, LocalDateTime.of(2026, 7, 20, 8, 0), new BigDecimal("163"), new BigDecimal("10")));
        when(refuelingRecordsRepository.findByFechaRegistroBetweenAndStatusTrue(any(), any())).thenReturn(tanqueos);

        byte[] excel = service.exportarMensual(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        Sheet sheet = leer(excel).getSheet("2026-07");
        Row fila = sheet.getRow(2);
        assertEquals("Retro Cat 02", fila.getCell(0).getStringCellValue());
        assertEquals("1,50 H/Gl", fila.getCell(1).getStringCellValue());
        assertEquals(100.0, fila.getCell(2).getNumericCellValue(), 0.01); // primero
        assertEquals(163.0, fila.getCell(3).getNumericCellValue(), 0.01); // último
        assertEquals(20.0, fila.getCell(4).getNumericCellValue(), 0.01);  // consumo = 10+10
        assertEquals(63.0, fila.getCell(5).getNumericCellValue(), 0.01);  // recorrido = 163-100
        assertEquals(3.15, fila.getCell(6).getNumericCellValue(), 0.01); // eficiencia = 63/20
    }

    @Test
    void horometroEnCeroSeTrataComoSinLectura_PeroElConsumoSiSuma() throws IOException {
        AssetFuelConfigEntity config = AssetFuelConfigEntity.builder()
                .id(1L).machineId(20L).consumoEstandar(new BigDecimal("2")).unidadConsumo("HORA_POR_GALON").build();
        when(assetFuelConfigRepository.findAll()).thenReturn(List.of(config));
        when(machineRepository.findById(20L)).thenReturn(java.util.Optional.of(
                MachineEntity.builder().id(20L).name("Volqueta 051").build()));

        List<RefuelingRecordsEntity> tanqueos = List.of(
                tanqueo(20L, LocalDateTime.of(2026, 7, 10, 8, 0), BigDecimal.ZERO, new BigDecimal("6")));
        when(refuelingRecordsRepository.findByFechaRegistroBetweenAndStatusTrue(any(), any())).thenReturn(tanqueos);

        byte[] excel = service.exportarMensual(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        Sheet sheet = leer(excel).getSheet("2026-07");
        Row fila = sheet.getRow(2);
        assertEquals("-", fila.getCell(2).getStringCellValue());
        assertEquals("-", fila.getCell(3).getStringCellValue());
        assertEquals(6.0, fila.getCell(4).getNumericCellValue(), 0.01);
        assertEquals("-", fila.getCell(5).getStringCellValue());
        assertEquals(0.0, fila.getCell(6).getNumericCellValue(), 0.01);
    }

    @Test
    void siempreListaTodosLosActivosConfigurados_AunqueNoHayanTanqueadoEseMes() throws IOException {
        AssetFuelConfigEntity config = AssetFuelConfigEntity.builder()
                .id(1L).machineId(30L).consumoEstandar(new BigDecimal("15")).unidadConsumo("KM_POR_GALON").build();
        when(assetFuelConfigRepository.findAll()).thenReturn(List.of(config));
        when(machineRepository.findById(30L)).thenReturn(java.util.Optional.of(
                MachineEntity.builder().id(30L).name("Camión Hino").build()));
        when(refuelingRecordsRepository.findByFechaRegistroBetweenAndStatusTrue(any(), any())).thenReturn(List.of());

        byte[] excel = service.exportarMensual(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        Sheet sheet = leer(excel).getSheet("2026-07");
        // Sin tanqueos ese mes, el activo configurado igual aparece — con "-" en las
        // columnas de rendimiento en vez de desaparecer de la tabla.
        Row fila = sheet.getRow(2);
        assertEquals("Camión Hino", fila.getCell(0).getStringCellValue());
        assertEquals("15,00 Km/Gl", fila.getCell(1).getStringCellValue());
        assertEquals("-", fila.getCell(2).getStringCellValue());
        assertEquals(0.0, fila.getCell(4).getNumericCellValue(), 0.01);
    }

    @Test
    void tituloDeLaHojaEsElMesEnEspanol() throws IOException {
        when(assetFuelConfigRepository.findAll()).thenReturn(List.of());
        when(refuelingRecordsRepository.findByFechaRegistroBetweenAndStatusTrue(any(), any())).thenReturn(List.of());

        byte[] excel = service.exportarMensual(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        Sheet sheet = leer(excel).getSheet("2026-07");
        assertEquals("JULIO 2026", sheet.getRow(0).getCell(0).getStringCellValue());
    }
}

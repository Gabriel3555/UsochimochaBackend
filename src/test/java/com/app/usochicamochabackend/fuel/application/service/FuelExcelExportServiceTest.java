package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.fuel.application.dto.FuelBudgetProjectionRow;
import com.app.usochicamochabackend.fuel.application.dto.RefuelingRecordResponse;
import com.app.usochicamochabackend.fuel.application.port.GetFuelDashboardUseCase;
import com.app.usochicamochabackend.fuel.application.port.GetRefuelingReportUseCase;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FuelExcelExportServiceTest {

    @Mock
    private GetRefuelingReportUseCase getRefuelingReportUseCase;

    @Mock
    private GetFuelDashboardUseCase getFuelDashboardUseCase;

    private FuelExcelExportService fuelExcelExportService;

    @BeforeEach
    void setUp() {
        fuelExcelExportService = new FuelExcelExportService(getRefuelingReportUseCase, getFuelDashboardUseCase);
    }

    private RefuelingRecordResponse tanqueo(Long id, boolean discrepanciaValor, boolean capacidadExcedida) {
        return new RefuelingRecordResponse(
                id, 5, null, "BOMBA", "DISTRITO", 1L,
                new BigDecimal("10"), new BigDecimal("500"), false, new BigDecimal("10000"),
                null, new BigDecimal("100000"), new BigDecimal("100000"), discrepanciaValor,
                null, null, 1L, LocalDateTime.of(2026, 7, 15, 10, 0),
                capacidadExcedida, false, false, false,
                BigDecimal.ZERO, null, null, null, null
        );
    }

    private Workbook leer(byte[] excel) throws IOException {
        return new XSSFWorkbook(new ByteArrayInputStream(excel));
    }

    @Test
    void generaExactamenteDosHojas_TanqueosYProyeccionPresupuestal() throws IOException {
        when(getRefuelingReportUseCase.obtenerReporte(any(), any(), any(), any())).thenReturn(List.of());
        when(getFuelDashboardUseCase.obtenerProyeccionPresupuestal(any())).thenReturn(List.of());

        byte[] excel = fuelExcelExportService.exportarExcel("VEHICULO", null, null, null);

        Workbook workbook = leer(excel);
        assertEquals(2, workbook.getNumberOfSheets());
        assertNotNull(workbook.getSheet("Tanqueos"));
        assertNotNull(workbook.getSheet("Proyección Presupuestal"));
    }

    @Test
    void hojaTanqueos_TieneEncabezadoMasUnaFilaPorTanqueo() throws IOException {
        when(getRefuelingReportUseCase.obtenerReporte(any(), any(), any(), any()))
                .thenReturn(List.of(tanqueo(1L, false, false), tanqueo(2L, false, false), tanqueo(3L, false, false)));
        when(getFuelDashboardUseCase.obtenerProyeccionPresupuestal(any())).thenReturn(List.of());

        byte[] excel = fuelExcelExportService.exportarExcel("VEHICULO", null, null, null);

        Sheet sheet = leer(excel).getSheet("Tanqueos");
        // fila 0 = encabezado, filas 1..3 = datos -> última fila con datos es índice 3
        assertEquals(3, sheet.getLastRowNum());
    }

    @Test
    void hojaTanqueos_VuelcaBooleanosDeAnomaliaComoSiONo() throws IOException {
        when(getRefuelingReportUseCase.obtenerReporte(any(), any(), any(), any()))
                .thenReturn(List.of(tanqueo(1L, true, false), tanqueo(2L, false, true)));
        when(getFuelDashboardUseCase.obtenerProyeccionPresupuestal(any())).thenReturn(List.of());

        byte[] excel = fuelExcelExportService.exportarExcel("VEHICULO", null, null, null);

        Sheet sheet = leer(excel).getSheet("Tanqueos");
        Row filaConDiscrepancia = sheet.getRow(1);
        Row filaConCapacidadExcedida = sheet.getRow(2);

        assertEquals("Sí", filaConDiscrepancia.getCell(9).getStringCellValue());
        assertEquals("No", filaConDiscrepancia.getCell(10).getStringCellValue());
        assertEquals("No", filaConCapacidadExcedida.getCell(9).getStringCellValue());
        assertEquals("Sí", filaConCapacidadExcedida.getCell(10).getStringCellValue());
    }

    @Test
    void hojaProyeccion_VuelcaLasFilasQueEntregaElUseCaseTalCual() throws IOException {
        // El cálculo del promedio/proyección ya no vive acá — se movió a
        // FuelDashboardService.obtenerProyeccionPresupuestal (compartido con el Dashboard
        // Financiero) y se prueba en FuelDashboardServiceTest. Acá solo se verifica que la
        // hoja escriba fielmente lo que el use case le entrega.
        when(getRefuelingReportUseCase.obtenerReporte(any(), any(), any(), any())).thenReturn(List.of());
        when(getFuelDashboardUseCase.obtenerProyeccionPresupuestal(any())).thenReturn(List.of(
                new FuelBudgetProjectionRow(LocalDate.of(2026, 7, 1), new BigDecimal("200000"), false),
                new FuelBudgetProjectionRow(LocalDate.of(2026, 9, 1), new BigDecimal("300000"), true)
        ));

        byte[] excel = fuelExcelExportService.exportarExcel("VEHICULO", null, null, null);

        Sheet sheet = leer(excel).getSheet("Proyección Presupuestal");
        Row historica = sheet.getRow(1);
        Row proyectada = sheet.getRow(2);

        assertEquals("2026-07", historica.getCell(0).getStringCellValue());
        assertEquals(200000.0, historica.getCell(1).getNumericCellValue(), 0.01);
        assertEquals("Histórico", historica.getCell(2).getStringCellValue());

        assertEquals("2026-09", proyectada.getCell(0).getStringCellValue());
        assertEquals(300000.0, proyectada.getCell(1).getNumericCellValue(), 0.01);
        assertEquals("Proyectado", proyectada.getCell(2).getStringCellValue());
    }
}

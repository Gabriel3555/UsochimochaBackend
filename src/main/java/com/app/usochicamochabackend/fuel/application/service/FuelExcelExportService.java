package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.fuel.application.dto.FuelBudgetProjectionRow;
import com.app.usochicamochabackend.fuel.application.dto.RefuelingRecordResponse;
import com.app.usochicamochabackend.fuel.application.port.ExportRefuelingReportUseCase;
import com.app.usochicamochabackend.fuel.application.port.GetFuelDashboardUseCase;
import com.app.usochicamochabackend.fuel.application.port.GetRefuelingReportUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Exportación a Excel del reporte de "Tanqueo y Distribución" (detalle completo, no
 * la vista colapsada de 1 fila por activo), más la hoja de proyección presupuestal
 * calculada por {@link GetFuelDashboardUseCase#obtenerProyeccionPresupuestal}
 * (compartida con el Dashboard Financiero). Sustituto acordado con el usuario de la
 * "auditoría física y monetaria" del cronograma original: el sistema no audita por
 * sí mismo, pero entrega los datos reales para revisión manual por parte de
 * Asociación/Distrito.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FuelExcelExportService implements ExportRefuelingReportUseCase {

    private static final DateTimeFormatter FECHA_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter MES_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final GetRefuelingReportUseCase getRefuelingReportUseCase;
    private final GetFuelDashboardUseCase getFuelDashboardUseCase;

    @Override
    public byte[] exportarExcel(String tipo, String area, LocalDate fechaInicio, LocalDate fechaFin) {
        List<RefuelingRecordResponse> tanqueos = getRefuelingReportUseCase.obtenerReporte(tipo, area, fechaInicio, fechaFin);
        List<FuelBudgetProjectionRow> proyeccion = getFuelDashboardUseCase.obtenerProyeccionPresupuestal(fechaFin);

        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = crearHeaderStyle(workbook);
            CellStyle dataStyle = crearDataStyle(workbook);

            escribirHojaTanqueos(workbook, headerStyle, dataStyle, tanqueos);
            escribirHojaProyeccion(workbook, headerStyle, dataStyle, proyeccion);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            log.info("Excel de tanqueos generado: {} tanqueos, {} filas de proyección", tanqueos.size(), proyeccion.size());
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el archivo Excel de tanqueos.", e);
        }
    }

    private void escribirHojaTanqueos(Workbook workbook, CellStyle headerStyle, CellStyle dataStyle,
            List<RefuelingRecordResponse> tanqueos) {
        Sheet sheet = workbook.createSheet("Tanqueos");
        String[] headers = {
                "Fecha", "Activo (Tipo)", "Activo (Nombre)", "Lugar", "Área de costo",
                "Cantidad (gal)", "Precio unitario", "Total calculado", "Total ingresado",
                "Discrepancia valor", "Capacidad excedida", "Cantidad fuera de rango",
                "Precio fuera de rango", "Full inconsistente", "Cantidad reintegrada"
        };
        escribirEncabezado(sheet, headerStyle, headers);

        int rowNum = 1;
        for (RefuelingRecordResponse t : tanqueos) {
            Row row = sheet.createRow(rowNum++);
            int col = 0;
            row.createCell(col++).setCellValue(t.fechaRegistro() != null ? t.fechaRegistro().format(FECHA_FORMATTER) : "");
            row.createCell(col++).setCellValue(nullSafe(t.assetType()));
            row.createCell(col++).setCellValue(nullSafe(t.assetName()));
            row.createCell(col++).setCellValue(nullSafe(t.lugar()));
            row.createCell(col++).setCellValue(nullSafe(t.areaCosto()));
            row.createCell(col++).setCellValue(bigDecimalToString(t.cantidadGalones()));
            row.createCell(col++).setCellValue(bigDecimalToString(t.precioUnitario()));
            row.createCell(col++).setCellValue(bigDecimalToString(t.totalCalculado()));
            row.createCell(col++).setCellValue(bigDecimalToString(t.totalIngresado()));
            row.createCell(col++).setCellValue(siONo(t.discrepanciaValor()));
            row.createCell(col++).setCellValue(siONo(t.capacidadExcedida()));
            row.createCell(col++).setCellValue(siONo(t.cantidadFueraDeRango()));
            row.createCell(col++).setCellValue(siONo(t.precioFueraDeRango()));
            row.createCell(col++).setCellValue(siONo(t.fullInconsistente()));
            row.createCell(col).setCellValue(bigDecimalToString(t.cantidadReintegrada()));
            aplicarEstiloFila(row, dataStyle, headers.length);
        }
        autoAjustarColumnas(sheet, headers.length);
    }

    private void escribirHojaProyeccion(Workbook workbook, CellStyle headerStyle, CellStyle dataStyle,
            List<FuelBudgetProjectionRow> proyeccion) {
        Sheet sheet = workbook.createSheet("Proyección Presupuestal");
        String[] headers = {"Mes", "Gasto neto", "Tipo"};
        escribirEncabezado(sheet, headerStyle, headers);

        int rowNum = 1;
        for (FuelBudgetProjectionRow fila : proyeccion) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(fila.mes().format(MES_FORMATTER));
            row.createCell(1).setCellValue(fila.gastoNeto().doubleValue());
            row.createCell(2).setCellValue(fila.proyectado() ? "Proyectado" : "Histórico");
            aplicarEstiloFila(row, dataStyle, headers.length);
        }
        autoAjustarColumnas(sheet, headers.length);
    }

    private void escribirEncabezado(Sheet sheet, CellStyle headerStyle, String[] headers) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void aplicarEstiloFila(Row row, CellStyle dataStyle, int columnas) {
        for (int i = 0; i < columnas; i++) {
            row.getCell(i).setCellStyle(dataStyle);
        }
    }

    private void autoAjustarColumnas(Sheet sheet, int columnas) {
        for (int i = 0; i < columnas; i++) {
            sheet.autoSizeColumn(i);
            if (sheet.getColumnWidth(i) < 3000) {
                sheet.setColumnWidth(i, 3000);
            }
            if (sheet.getColumnWidth(i) > 8000) {
                sheet.setColumnWidth(i, 8000);
            }
        }
    }

    private CellStyle crearHeaderStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        return headerStyle;
    }

    private CellStyle crearDataStyle(Workbook workbook) {
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        return dataStyle;
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    private String bigDecimalToString(BigDecimal value) {
        return value != null ? value.toString() : "";
    }

    private String siONo(Boolean value) {
        return Boolean.TRUE.equals(value) ? "Sí" : "No";
    }
}

package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.fuel.application.dto.FuelPerformanceResponse;
import com.app.usochicamochabackend.fuel.application.port.ExportFuelPerformanceHistoryUseCase;
import com.app.usochicamochabackend.fuel.application.port.GetFuelPerformanceUseCase;
import com.app.usochicamochabackend.fuel.infrastructure.entity.AssetFuelConfigEntity;
import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelTypesEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.AssetFuelConfigRepository;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelTypesRepository;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Exportación a Excel del historial de rendimiento de UN activo puntual — pantalla
 * "Historial de rendimiento" del front (FuelPerformanceHistory.svelte). Una hoja por
 * mes, con las mismas columnas que la tabla "Historial detallado" en pantalla (ver
 * createFuelPerformanceColumns en el front).
 *
 * Reutiliza {@link GetFuelPerformanceUseCase#obtenerRendimiento} — la misma fuente
 * que ya consume esa pantalla — y filtra al activo pedido. La config del activo
 * (tanque/unidad de consumo) es constante para todas las filas de un mismo activo,
 * así que se resuelve una sola vez en vez de por fila (a diferencia del reporte de
 * tanqueos, donde cada fila puede ser de un activo distinto).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FuelPerformanceHistoryExcelExportService implements ExportFuelPerformanceHistoryUseCase {

    private static final String MAQUINARIA = "MAQUINARIA";
    private static final String VEHICULO = "VEHICULO";
    private static final String MOTOCICLETA = "MOTOCICLETA";

    // Mismo criterio de "todo el histórico" que FECHA_INICIO_HISTORICO en
    // FuelPerformanceHistory.svelte: el backend de Rendimiento asume el mes actual
    // si las fechas vienen vacías, así que acá se fuerza un rango amplio explícito.
    private static final LocalDate FECHA_INICIO_HISTORICO = LocalDate.of(2000, 1, 1);

    private static final DateTimeFormatter FECHA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    // Nombre de hoja: yyyy-MM (sin caracteres inválidos para Excel, ordena
    // cronológicamente solo con orden alfabético de las pestañas).
    private static final DateTimeFormatter MES_SHEET_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private static final Map<String, String> UNIDAD_CONSUMO_LABELS = Map.of(
            "KM_POR_GALON", "Km/Gl",
            "HORA_POR_GALON", "H/Gl",
            "KM_POR_M3", "Km/M3",
            "HORA_POR_M3", "H/M3");

    private final GetFuelPerformanceUseCase getFuelPerformanceUseCase;
    private final AssetFuelConfigRepository assetFuelConfigRepository;
    private final FuelTypesRepository fuelTypesRepository;

    @Override
    public byte[] exportarHistorialActivo(String tipo, Long activoId) {
        if (!MAQUINARIA.equals(tipo) && !VEHICULO.equals(tipo) && !MOTOCICLETA.equals(tipo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tipo debe ser MAQUINARIA, VEHICULO o MOTOCICLETA.");
        }
        boolean esMaquina = MAQUINARIA.equals(tipo);

        List<FuelPerformanceResponse> filas = getFuelPerformanceUseCase
                .obtenerRendimiento(tipo, FECHA_INICIO_HISTORICO, LocalDate.now()).stream()
                .filter(r -> esMaquina ? activoId.equals(r.machineId()) : activoId.intValue() == safeInt(r.vehicleId()))
                .sorted(Comparator.comparing(FuelPerformanceResponse::fechaRegistro))
                .toList();
        if (filas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No hay historial de rendimiento para este activo.");
        }

        Optional<AssetFuelConfigEntity> config = esMaquina
                ? assetFuelConfigRepository.findByMachineId(activoId)
                : assetFuelConfigRepository.findByVehicleId(activoId.intValue());
        BigDecimal tanqueCapacidadGal = config.map(AssetFuelConfigEntity::getTanqueCapacidadGal).orElse(null);
        String unidadConsumo = config.map(AssetFuelConfigEntity::getUnidadConsumo).orElse(null);
        String unidadFisica = config.map(AssetFuelConfigEntity::getFuelTypeDefaultId)
                .flatMap(fuelTypesRepository::findById)
                .map(FuelTypesEntity::getUnidadMedida)
                .orElse("GALON");

        Set<Long> fuelTypeIds = filas.stream().map(FuelPerformanceResponse::fuelTypeId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> nombreFuelTypeById = fuelTypesRepository.findAllById(fuelTypeIds).stream()
                .collect(Collectors.toMap(FuelTypesEntity::getId, FuelTypesEntity::getNombre));

        Map<YearMonth, List<FuelPerformanceResponse>> porMes = filas.stream()
                .collect(Collectors.groupingBy(f -> YearMonth.from(f.fechaRegistro()), TreeMap::new, Collectors.toList()));

        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = crearHeaderStyle(workbook);
            CellStyle dataStyle = crearDataStyle(workbook);

            for (Map.Entry<YearMonth, List<FuelPerformanceResponse>> entry : porMes.entrySet()) {
                escribirHojaMes(workbook, headerStyle, dataStyle, entry.getKey(), entry.getValue(),
                        esMaquina, tanqueCapacidadGal, unidadConsumo, unidadFisica, nombreFuelTypeById);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            log.info("Excel de historial de rendimiento generado: activo {} ({}), {} meses, {} filas",
                    activoId, tipo, porMes.size(), filas.size());
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el archivo Excel de historial de rendimiento.", e);
        }
    }

    private void escribirHojaMes(Workbook workbook, CellStyle headerStyle, CellStyle dataStyle, YearMonth mes,
            List<FuelPerformanceResponse> filas, boolean esMaquina, BigDecimal tanqueCapacidadGal,
            String unidadConsumo, String unidadFisica, Map<Long, String> nombreFuelTypeById) {
        String unidadMedicion = esMaquina ? "Horómetro" : "Kilometraje";
        String unidadEjec = esMaquina ? "Horas" : "Km";
        String sufijo = esMaquina ? "h" : "km";
        String unidadFisicaLabel = "M3".equals(unidadFisica) ? "m³" : "gal";

        Sheet sheet = workbook.createSheet(mes.format(MES_SHEET_FORMATTER));
        String[] headers = {
                "Activo", "Producto", "Fecha",
                "Consumo estándar (A)", "Tamaño tanque (gal)",
                "Total tanqueado, último registro (F)",
                unidadMedicion + " anterior (B)", unidadMedicion + " actual (C)",
                unidadEjec + " esperadas por el combustible (H = F×A)",
                unidadEjec + " ejecutadas (D = C−B)",
                "Diferencia ejecutado / esperado (D − H)",
                "% Eficiencia (D ÷ H)", "¿Tanque lleno?", "Alerta"
        };
        escribirEncabezado(sheet, headerStyle, headers);

        int rowNum = 1;
        for (FuelPerformanceResponse f : filas) {
            BigDecimal esperado = nullToZero(f.galonesReal()).multiply(nullToZero(f.consumoEstandar()));
            BigDecimal ejecutado = nullToZero(f.ejecutado());
            BigDecimal diferencia = ejecutado.subtract(esperado);
            String eficiencia = esperado.compareTo(BigDecimal.ZERO) != 0
                    ? fmt2(ejecutado.divide(esperado, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))) + "%"
                    : "—";

            Row row = sheet.createRow(rowNum++);
            int col = 0;
            row.createCell(col++).setCellValue(nullSafe(f.identificacionActivo()));
            row.createCell(col++).setCellValue(nullSafe(nombreFuelTypeById.get(f.fuelTypeId())));
            row.createCell(col++).setCellValue(f.fechaRegistro() != null ? f.fechaRegistro().format(FECHA_FORMATTER) : "");
            row.createCell(col++).setCellValue(fmt2(f.consumoEstandar()) + " " + unidadConsumoLabel(unidadConsumo));
            row.createCell(col++).setCellValue(tanqueCapacidadGal != null ? fmt2(tanqueCapacidadGal) + " gal" : "—");
            row.createCell(col++).setCellValue(fmt2(f.galonesReal()) + " " + unidadFisicaLabel);
            row.createCell(col++).setCellValue(fmt2(f.horometroAnterior()) + " " + sufijo);
            row.createCell(col++).setCellValue(fmt2(f.horometroActual()) + " " + sufijo);
            row.createCell(col++).setCellValue(fmt2(esperado) + " " + sufijo);
            row.createCell(col++).setCellValue(fmt2(ejecutado) + " " + sufijo);
            row.createCell(col++).setCellValue((diferencia.signum() >= 0 ? "+" : "−") + fmt2(diferencia.abs()) + " " + sufijo);
            row.createCell(col++).setCellValue(eficiencia);
            row.createCell(col++).setCellValue(siNo(f.esFull()));
            row.createCell(col).setCellValue(siNo(f.alerta()) + (Boolean.FALSE.equals(f.usaRangoAprendido()) ? " *" : ""));
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

    private int safeInt(Integer value) {
        return value != null ? value : -1;
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    // Formato es-CO (punto de miles, coma decimal) — mismo criterio que
    // Intl.NumberFormat('es-CO', {minimumFractionDigits:2, maximumFractionDigits:2})
    // usado por fmt2 en el front, para que el número se lea igual en pantalla y en Excel.
    private String fmt2(BigDecimal value) {
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("es", "CO"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(nullToZero(value));
    }

    private String unidadConsumoLabel(String codigo) {
        if (codigo == null) return "—";
        return UNIDAD_CONSUMO_LABELS.getOrDefault(codigo, codigo);
    }

    // Sí/No/N/A — mismo criterio de 3 estados que yn() en el front (a diferencia de
    // siONo en FuelExcelExportService, que solo distingue Sí/No para un DTO donde el
    // campo nunca es null).
    private String siNo(Boolean value) {
        if (Boolean.TRUE.equals(value)) return "SÍ";
        if (Boolean.FALSE.equals(value)) return "NO";
        return "N/A";
    }
}

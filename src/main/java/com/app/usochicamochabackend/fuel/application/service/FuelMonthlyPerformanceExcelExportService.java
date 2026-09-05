package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.fuel.application.port.ExportFuelMonthlyPerformanceUseCase;
import com.app.usochicamochabackend.fuel.infrastructure.entity.AssetFuelConfigEntity;
import com.app.usochicamochabackend.fuel.infrastructure.entity.RefuelingRecordsEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.AssetFuelConfigRepository;
import com.app.usochicamochabackend.fuel.infrastructure.repository.RefuelingRecordsRepository;
import com.app.usochicamochabackend.machine.infrastructure.entity.MachineEntity;
import com.app.usochicamochabackend.machine.infrastructure.repository.MachineRepository;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleRepository;
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
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Exportación a Excel del rendimiento mensual de TODOS los activos configurados
 * (pantalla Rendimiento — botón "Exportar Excel" en FuelPerformance.svelte, distinto
 * del export de historial de UN activo que ya existía en
 * {@link FuelPerformanceHistoryExcelExportService}).
 *
 * Una hoja por mes calendario dentro del rango pedido — incluyendo meses sin ningún
 * tanqueo, para que la descarga nunca "salte" un mes — con UNA sola tabla por activo:
 * nombre + consumo estándar configurado (asset_fuel_config, fijo mes a mes) junto al
 * rendimiento real de ese mes (horómetro/km primero y último tanqueo, consumo total,
 * recorrido y eficiencia real). Antes eran dos tablas lado a lado emparejadas solo por
 * posición de fila (sin nombre en la de rendimiento) — se rompía en cuanto un activo
 * no tenía tanqueos ese mes, porque esa tabla se acortaba y la de configuración no.
 * Ahora TODOS los activos configurados aparecen siempre, con "-" en las columnas de
 * rendimiento cuando no hay tanqueos ese mes, así el archivo nunca deja de "cuadrar".
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FuelMonthlyPerformanceExcelExportService implements ExportFuelMonthlyPerformanceUseCase {

    private static final DateTimeFormatter MES_TITULO_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("es", "ES"));
    private static final DateTimeFormatter MES_SHEET_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int NUM_COLUMNAS = 7;

    private static final Map<String, String> UNIDAD_CONSUMO_LABELS = Map.of(
            "KM_POR_GALON", "Km/Gl",
            "HORA_POR_GALON", "H/Gl",
            "KM_POR_M3", "Km/M3",
            "HORA_POR_M3", "H/M3");

    private final RefuelingRecordsRepository refuelingRecordsRepository;
    private final AssetFuelConfigRepository assetFuelConfigRepository;
    private final VehicleRepository vehicleRepository;
    private final MachineRepository machineRepository;

    private record InfoActivo(String tipo, String nombre) {}

    private record FilaActivoMes(String nombre, String consumoEstandarLabel, BigDecimal primero, BigDecimal ultimo,
            BigDecimal consumo, BigDecimal recorrido, BigDecimal eficiencia) {}

    @Override
    public byte[] exportarMensual(LocalDate fechaInicio, LocalDate fechaFin) {
        LocalDate hoy = LocalDate.now();
        LocalDate inicio = fechaInicio != null ? fechaInicio : hoy.withDayOfYear(1);
        LocalDate fin = fechaFin != null ? fechaFin : hoy;
        Timestamp inicioTs = Timestamp.valueOf(LocalDateTime.of(inicio, LocalTime.MIN));
        Timestamp finTs = Timestamp.valueOf(LocalDateTime.of(fin, LocalTime.MAX));

        List<RefuelingRecordsEntity> tanqueos = refuelingRecordsRepository.findByFechaRegistroBetweenAndStatusTrue(inicioTs, finTs);
        // Todos los activos con consumo estándar configurado, sin importar tipo — el
        // export mensual no depende de qué pestaña (Maquinaria/Vehículo/Motocicleta)
        // esté activa en pantalla, siempre trae los 3 tipos juntos.
        List<AssetFuelConfigEntity> configs = assetFuelConfigRepository.findAll();

        Map<String, InfoActivo> infoPorClave = new LinkedHashMap<>();
        for (AssetFuelConfigEntity c : configs) {
            infoPorClave.put(clave(c.getVehicleId(), c.getMachineId()), resolverInfo(c));
        }
        List<AssetFuelConfigEntity> configsOrdenados = configs.stream()
                .sorted(Comparator
                        .comparing((AssetFuelConfigEntity c) -> infoPorClave.get(clave(c.getVehicleId(), c.getMachineId())).tipo())
                        .thenComparing(c -> infoPorClave.get(clave(c.getVehicleId(), c.getMachineId())).nombre()))
                .toList();

        Map<YearMonth, List<RefuelingRecordsEntity>> tanqueosPorMes = tanqueos.stream()
                .collect(Collectors.groupingBy(t -> YearMonth.from(t.getFechaRegistro().toLocalDateTime())));

        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = crearHeaderStyle(workbook);
            CellStyle tituloStyle = crearTituloStyle(workbook);
            CellStyle dataStyle = crearDataStyle(workbook);
            CellStyle numeroStyle = crearNumeroStyle(workbook);

            YearMonth mesInicio = YearMonth.from(inicio);
            YearMonth mesFin = YearMonth.from(fin);
            for (YearMonth mes = mesInicio; !mes.isAfter(mesFin); mes = mes.plusMonths(1)) {
                List<FilaActivoMes> filas = calcularFilasDelMes(tanqueosPorMes.getOrDefault(mes, List.of()),
                        configsOrdenados, infoPorClave);
                escribirHojaMes(workbook, headerStyle, tituloStyle, dataStyle, numeroStyle, mes, filas);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            log.info("Excel de rendimiento mensual generado: {} a {}, {} activos", mesInicio, mesFin, configsOrdenados.size());
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el archivo Excel de rendimiento mensual.", e);
        }
    }

    private List<FilaActivoMes> calcularFilasDelMes(List<RefuelingRecordsEntity> tanqueosDelMes,
            List<AssetFuelConfigEntity> configsOrdenados, Map<String, InfoActivo> infoPorClave) {
        Map<String, List<RefuelingRecordsEntity>> porActivo = tanqueosDelMes.stream()
                .collect(Collectors.groupingBy(t -> clave(t.getVehicleId(), t.getMachineId())));

        List<FilaActivoMes> filas = new ArrayList<>();
        for (AssetFuelConfigEntity cfg : configsOrdenados) {
            String key = clave(cfg.getVehicleId(), cfg.getMachineId());
            List<RefuelingRecordsEntity> delActivo = porActivo.getOrDefault(key, List.of()).stream()
                    .sorted(Comparator.comparing(RefuelingRecordsEntity::getFechaRegistro))
                    .toList();

            BigDecimal consumo = delActivo.stream().map(RefuelingRecordsEntity::getCantidadGalones)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Un horómetro/km en 0 se trata como "sin lectura registrada" (tanqueo
            // cargado solo por el consumo, sin anotar la lectura real) — no cuenta
            // para primero/último ni para el recorrido, pero el consumo sí suma.
            List<BigDecimal> lecturas = delActivo.stream()
                    .map(RefuelingRecordsEntity::getHorometroKm)
                    .filter(h -> h != null && h.signum() > 0)
                    .toList();
            BigDecimal primero = lecturas.isEmpty() ? null : lecturas.get(0);
            BigDecimal ultimo = lecturas.isEmpty() ? null : lecturas.get(lecturas.size() - 1);
            BigDecimal recorrido = (primero != null && ultimo != null) ? ultimo.subtract(primero) : null;
            BigDecimal eficiencia = (recorrido != null && consumo.compareTo(BigDecimal.ZERO) != 0)
                    ? recorrido.divide(consumo, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            filas.add(new FilaActivoMes(infoPorClave.get(key).nombre(), formatoConsumoEstandar(cfg),
                    primero, ultimo, consumo, recorrido, eficiencia));
        }
        return filas;
    }

    private void escribirHojaMes(Workbook workbook, CellStyle headerStyle, CellStyle tituloStyle, CellStyle dataStyle,
            CellStyle numeroStyle, YearMonth mes, List<FilaActivoMes> filas) {
        Sheet sheet = workbook.createSheet(mes.format(MES_SHEET_FORMATTER));

        Row tituloRow = sheet.createRow(0);
        Cell tituloCell = tituloRow.createCell(0);
        tituloCell.setCellValue(mes.format(MES_TITULO_FORMATTER).toUpperCase());
        tituloCell.setCellStyle(tituloStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, NUM_COLUMNAS - 1));

        String[] headers = {
                "MAQUINARIA//VEHICULO", "CONSUMO ESTANDAR",
                "HORAS/KILOMETRAJE PRIMER TANQUEO MES", "HORAS/KILOMETRAJE ULTIMO TANQUEO MES",
                "CONSUMO COMBUSTIBLE", "RECORRIDO MES", "CONSUMO * HORA/GALON"
        };
        Row headerRow = sheet.createRow(1);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 2;
        for (FilaActivoMes f : filas) {
            Row row = sheet.createRow(rowNum++);
            Cell nombreCell = row.createCell(0);
            nombreCell.setCellValue(f.nombre());
            nombreCell.setCellStyle(dataStyle);
            Cell consumoEstandarCell = row.createCell(1);
            consumoEstandarCell.setCellValue(f.consumoEstandarLabel());
            consumoEstandarCell.setCellStyle(dataStyle);
            escribirNumeroOGuion(row, 2, f.primero(), numeroStyle, dataStyle);
            escribirNumeroOGuion(row, 3, f.ultimo(), numeroStyle, dataStyle);
            escribirNumero(row, 4, f.consumo(), numeroStyle);
            escribirNumeroOGuion(row, 5, f.recorrido(), numeroStyle, dataStyle);
            escribirNumero(row, 6, f.eficiencia(), numeroStyle);
        }

        autoAjustarColumnas(sheet, NUM_COLUMNAS);
    }

    private void escribirNumero(Row row, int col, BigDecimal valor, CellStyle numeroStyle) {
        Cell cell = row.createCell(col);
        cell.setCellValue(valor != null ? valor.doubleValue() : 0d);
        cell.setCellStyle(numeroStyle);
    }

    private void escribirNumeroOGuion(Row row, int col, BigDecimal valor, CellStyle numeroStyle, CellStyle dataStyle) {
        Cell cell = row.createCell(col);
        if (valor != null) {
            cell.setCellValue(valor.doubleValue());
            cell.setCellStyle(numeroStyle);
        } else {
            cell.setCellValue("-");
            cell.setCellStyle(dataStyle);
        }
    }

    private void autoAjustarColumnas(Sheet sheet, int columnas) {
        for (int i = 0; i < columnas; i++) {
            sheet.autoSizeColumn(i);
            if (sheet.getColumnWidth(i) < 3000) {
                sheet.setColumnWidth(i, 3000);
            }
            if (sheet.getColumnWidth(i) > 9000) {
                sheet.setColumnWidth(i, 9000);
            }
        }
    }

    private String clave(Integer vehicleId, Long machineId) {
        return machineId != null ? "M-" + machineId : "V-" + vehicleId;
    }

    private InfoActivo resolverInfo(AssetFuelConfigEntity cfg) {
        if (cfg.getMachineId() != null) {
            String nombre = machineRepository.findById(cfg.getMachineId())
                    .map(MachineEntity::getName).orElse("Máquina #" + cfg.getMachineId());
            return new InfoActivo("Máquina", nombre);
        }
        return vehicleRepository.findById(cfg.getVehicleId())
                .map(v -> {
                    boolean esMoto = v.getTipoVehiculo() != null && "MOTOCICLETA".equalsIgnoreCase(v.getTipoVehiculo().getNombreTipo());
                    String tipo = esMoto ? "Motocicleta" : "Vehículo";
                    String marca = v.getMarca() != null ? v.getMarca().getDescripcion() : null;
                    String nombre = marca != null ? v.getPlaca() + " — " + marca : v.getPlaca();
                    return new InfoActivo(tipo, nombre);
                })
                .orElse(new InfoActivo("Vehículo", "Vehículo #" + cfg.getVehicleId()));
    }

    private String formatoConsumoEstandar(AssetFuelConfigEntity cfg) {
        String unidad = UNIDAD_CONSUMO_LABELS.getOrDefault(cfg.getUnidadConsumo(), cfg.getUnidadConsumo());
        return fmt2(cfg.getConsumoEstandar()) + " " + unidad;
    }

    private String fmt2(BigDecimal value) {
        java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(new Locale("es", "CO"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(value != null ? value : BigDecimal.ZERO);
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
        headerStyle.setWrapText(true);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        return headerStyle;
    }

    private CellStyle crearTituloStyle(Workbook workbook) {
        CellStyle tituloStyle = workbook.createCellStyle();
        Font tituloFont = workbook.createFont();
        tituloFont.setBold(true);
        tituloFont.setFontHeightInPoints((short) 12);
        tituloStyle.setFont(tituloFont);
        tituloStyle.setAlignment(HorizontalAlignment.CENTER);
        tituloStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        tituloStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        tituloStyle.setBorderBottom(BorderStyle.THIN);
        tituloStyle.setBorderTop(BorderStyle.THIN);
        tituloStyle.setBorderRight(BorderStyle.THIN);
        tituloStyle.setBorderLeft(BorderStyle.THIN);
        return tituloStyle;
    }

    private CellStyle crearDataStyle(Workbook workbook) {
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        return dataStyle;
    }

    private CellStyle crearNumeroStyle(Workbook workbook) {
        CellStyle numeroStyle = workbook.createCellStyle();
        numeroStyle.cloneStyleFrom(crearDataStyle(workbook));
        numeroStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
        return numeroStyle;
    }
}

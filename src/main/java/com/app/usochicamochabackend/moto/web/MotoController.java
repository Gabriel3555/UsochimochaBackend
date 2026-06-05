package com.app.usochicamochabackend.moto.web;

import com.app.usochicamochabackend.moto.application.dto.*;
import com.app.usochicamochabackend.moto.application.port.MotoMonitoringUseCase;
import com.app.usochicamochabackend.moto.application.service.MotoService;
import com.app.usochicamochabackend.update.application.service.ExcelGenerationService;
import com.app.usochicamochabackend.vehicle.application.dto.VehicleRequest;
import com.app.usochicamochabackend.vehicle.application.dto.VehicleResponse;
import com.app.usochicamochabackend.vehicleinspection.application.dto.VehicleInspectionReportDTO;
import com.app.usochicamochabackend.vehicleinspection.application.port.GetVehicleInspectionsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.List;

@RestController
@RequestMapping("/api/v1/moto")
@RequiredArgsConstructor
@Tag(
                name = "Moto",
                description = "Motocicletas: **CRUD** sobre la tabla `vehiculos` (tipo forzado a MOTOCICLETA en servidor), "
                                + "**inspección diaria** (`POST /inspeccion`), **documentos** por placa, **monitoreo** consolidado, "
                                + "**reportes** de inspección e **imagen** pública de documentos. "
                                + "Inspecciones formales tipo “preoperativa extendida” de automóvil usan `/api/v1/vehicle-inspection`.")
public class MotoController {

    private final MotoService motoService;
    private final MotoMonitoringUseCase monitoringUseCase;
    private final GetVehicleInspectionsUseCase getInspectionsUseCase;
    private final ExcelGenerationService excelGenerationService;

    @GetMapping("/placas")
    @Operation(summary = "Obtener motocicletas activas", description = "Retorna la lista de placas de motocicletas activas en la BD")
    public ResponseEntity<List<MotoPlacaResponse>> getMotocicletas() {
        return ResponseEntity.ok(motoService.getMotocicletas());
    }

    @GetMapping("/ubicaciones")
    @Operation(summary = "Obtener ubicaciones activas", description = "Retorna la lista de ubicaciones activas en la BD")
    public ResponseEntity<List<UbicacionResponse>> getUbicaciones() {
        return ResponseEntity.ok(motoService.getUbicaciones());
    }

    @GetMapping("/{placa}")
    @Operation(summary = "Obtener moto por placa", description = "Detalle completo de la moto por placa (404 si no existe o no está activa)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Moto encontrada"),
            @ApiResponse(responseCode = "404", description = "Moto no encontrada")
    })
    public ResponseEntity<VehicleResponse> getMotoByPlaca(@PathVariable String placa) {
        return ResponseEntity.ok(motoService.findMotoByPlaca(placa));
    }

    @GetMapping("/{placa}/documentos")
    @Operation(summary = "Documentos existentes de una moto", description = "Devuelve documentos + estado de última inspección para el pre-llenado")
    public ResponseEntity<List<DocumentoExistenteResponse>> getDocumentos(@PathVariable String placa) {
        return ResponseEntity.ok(motoService.getDocumentosByPlaca(placa));
    }

    @PostMapping("/inspeccion")
    @Operation(
                    summary = "Guardar inspección de moto",
                    description = "Registra cabecera en `inspeccion_pre_operativa`, detalle mecánico y detalle documentos. "
                                    + "Actualiza km del vehículo si se informa kilometraje. El responsable se toma del **usuario JWT**. "
                                    + "Respuesta: **201 Created** con el id numérico de la inspección.")
    @ApiResponses({
                    @ApiResponse(responseCode = "201", description = "Inspección creada; cuerpo = id de inspección (Long)"),
                    @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
                    @ApiResponse(responseCode = "404", description = "Vehículo no encontrado")
    })
    public ResponseEntity<Long> saveInspeccion(@RequestBody InspeccionMotoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(motoService.saveInspeccion(req));
    }

    @GetMapping("/monitoring/consolidated")
    @Operation(summary = "Obtener monitoreo consolidado de motos", description = "Dashboard con SOAT, Tecno, Aceite y filtros para motos.")
    public ResponseEntity<List<MotoMonitoringDTO>> getConsolidatedMonitoring() {
        return ResponseEntity.ok(monitoringUseCase.getConsolidatedMonitoring());
    }

    @GetMapping("/monitoring/consolidated/export")
    @Operation(summary = "Exportar consolidado de motos a Excel")
    public ResponseEntity<byte[]> exportConsolidatedMonitoring() throws IOException {
        byte[] excelData = excelGenerationService.generateMotoConsolidatedExcel(
                monitoringUseCase.getConsolidatedMonitoring());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "consolidado_motos.xlsx");
        return ResponseEntity.ok().headers(headers).body(excelData);
    }

    @GetMapping("/inspections/export")
    @Operation(
                    summary = "Exportar inspecciones de motos a Excel",
                    description = "Descarga el historial completo de inspecciones de motocicletas en formato .xlsx.")
    @ApiResponses({
                    @ApiResponse(responseCode = "200", description = "Archivo Excel generado"),
                    @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<byte[]> exportMotoInspections() throws IOException {
        List<VehicleInspectionReportDTO> inspections = getInspectionsUseCase.getMotoInspectionsHistory();
        byte[] excelData = excelGenerationService.generateVehicleInspectionsExcel(inspections);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "inspecciones_motos.xlsx");
        return ResponseEntity.ok().headers(headers).body(excelData);
    }

    @GetMapping("/inspections/reports/history")
    @Operation(summary = "Historial completo de inspecciones de motos", description = "Todas las inspecciones diarias (más recientes primero).")
    public ResponseEntity<List<VehicleInspectionReportDTO>> getMotoInspectionsHistory() {
        return ResponseEntity.ok(getInspectionsUseCase.getMotoInspectionsHistory());
    }

    @GetMapping("/inspections/reports")
    @Operation(summary = "Último reporte por placa", description = "Una fila por placa: inspección más reciente (incluso si hay varios registros de vehículo para la misma moto).")
    public ResponseEntity<List<VehicleInspectionReportDTO>> getMotoInspectionsLatest() {
        return ResponseEntity.ok(getInspectionsUseCase.getMotoInspectionsLatestPerVehicle());
    }

    // --- CRUD --- (tipo MOTOCICLETA fijado en MotoService; body compatible con VehicleRequest)
    @GetMapping
    @Operation(
                    summary = "Listar motocicletas (inventario admin)",
                    description = "Solo registros activos cuyo tipo es **MOTOCICLETA**, con marca, km y ubicación base si existen.")
    public ResponseEntity<List<VehicleResponse>> getAllMotos() {
        return ResponseEntity.ok(motoService.findAllMotos());
    }

    @PostMapping
    @Operation(
                    summary = "Crear motocicleta",
                    description = "Body tipo `VehicleRequest`; `idTipoVehiculo` del cliente se ignora — el servidor asigna el id del tipo **MOTOCICLETA**. "
                                    + "Placa única en toda la tabla vehículos. Requiere rol **ADMIN**.")
    @ApiResponses({
                    @ApiResponse(responseCode = "201", description = "Moto creada"),
                    @ApiResponse(responseCode = "400", description = "Placa duplicada, placa vacía o ubicación inválida"),
                    @ApiResponse(responseCode = "500", description = "Tipo MOTOCICLETA no configurado en catálogo")
    })
    public ResponseEntity<VehicleResponse> createMoto(@RequestBody VehicleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(motoService.createMoto(request));
    }

    @PutMapping("/{id}")
    @Operation(
                    summary = "Actualizar motocicleta",
                    description = "El registro debe ser de tipo MOTOCICLETA; si no, 400. Requiere rol **ADMIN**.")
    @ApiResponses({
                    @ApiResponse(responseCode = "200", description = "Moto actualizada"),
                    @ApiResponse(responseCode = "400", description = "No es moto / placa duplicada / ubicación inválida"),
                    @ApiResponse(responseCode = "404", description = "Id no encontrado")
    })
    public ResponseEntity<VehicleResponse> updateMoto(@PathVariable Integer id, @RequestBody VehicleRequest request) {
        return ResponseEntity.ok(motoService.updateMoto(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(
                    summary = "Eliminar motocicleta",
                    description = "Borra la fila en `vehiculos` si es tipo MOTOCICLETA. Requiere rol **ADMIN**.")
    @ApiResponses({
                    @ApiResponse(responseCode = "204", description = "Eliminada"),
                    @ApiResponse(responseCode = "400", description = "El id no corresponde a una motocicleta"),
                    @ApiResponse(responseCode = "404", description = "No encontrada")
    })
    public ResponseEntity<Void> deleteMoto(@PathVariable Integer id) {
        motoService.deleteMoto(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/documento/imagen/{fileName:.+}")
    @Operation(summary = "Obtener imagen de documento", description = "Retorna el flujo de bytes de la imagen del documento")
    public ResponseEntity<Resource> getDocumentoImagen(@PathVariable String fileName) {
        try {
            Path path = Paths.get("uploads/documents").resolve(fileName);
            Resource resource = new UrlResource(path.toUri());
            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(resource);
            }
        } catch (Exception e) {
            // Log error
        }
        return ResponseEntity.notFound().build();
    }
}
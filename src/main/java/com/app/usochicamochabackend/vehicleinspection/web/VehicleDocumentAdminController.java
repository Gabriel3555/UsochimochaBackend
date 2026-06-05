package com.app.usochicamochabackend.vehicleinspection.web;

import com.app.usochicamochabackend.vehicleinspection.application.dto.VehicleDocumentRequest;
import com.app.usochicamochabackend.vehicleinspection.application.service.VehiculoInspectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/documents")
@RequiredArgsConstructor
@Tag(
        name = "Admin Documents",
        description = "Alta y actualización de documentación (SOAT, tecnomecánica, licencia, extintor) en vehículos. "
                + "Requiere rol **ADMIN**. JSON en `POST /api/v1/admin/documents` o multipart en `POST .../upload`.")
public class VehicleDocumentAdminController {

    private final VehiculoInspectionService vehiculoInspectionService;

    @PostMapping
    @Operation(summary = "Registrar vigencia (JSON)", description = "Crea una nueva versión de documento; desactiva la anterior del mismo tipo.")
    public ResponseEntity<Void> updateDocument(@RequestBody VehicleDocumentRequest request, Authentication authentication) {
        vehiculoInspectionService.saveDocument(
                request,
                authentication != null ? authentication.getName() : null);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Subir archivo de documento",
            description = "Partes: `file` (PDF o imagen), `idVehiculo` (texto), `tipoDocumento`, `fechaVencimiento` (yyyy-MM-dd, opcional para TARJETA_DE_PROPIEDAD). "
                    + "El archivo anterior `current` pasa a la subcarpeta `archive/`.")
    public ResponseEntity<Void> uploadDocument(
            @RequestPart("file") MultipartFile file,
            @RequestPart("idVehiculo") String idVehiculoStr,
            @RequestPart("tipoDocumento") String tipoDocumento,
            @RequestPart(value = "fechaVencimiento", required = false) String fechaVencimientoStr,
            Authentication authentication) throws IOException {
        int idVehiculo = Integer.parseInt(idVehiculoStr.trim());
        LocalDate fecha = (fechaVencimientoStr != null && !fechaVencimientoStr.isBlank())
                ? LocalDate.parse(fechaVencimientoStr.trim())
                : null;
        vehiculoInspectionService.saveDocumentFromUpload(
                idVehiculo,
                tipoDocumento,
                fecha,
                file,
                authentication != null ? authentication.getName() : null);
        return ResponseEntity.ok().build();
    }
}

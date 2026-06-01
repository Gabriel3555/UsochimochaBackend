package com.app.usochicamochabackend.fuel.web;

import com.app.usochicamochabackend.fuel.application.dto.FuelAssetConfigRequest;
import com.app.usochicamochabackend.fuel.application.dto.FuelAssetConfigResponse;
import com.app.usochicamochabackend.fuel.application.port.GetFuelAssetConfigUseCase;
import com.app.usochicamochabackend.fuel.application.port.UpsertFuelAssetConfigUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fuel/config")
@RequiredArgsConstructor
@Tag(name = "Combustibles - Configuración", description = "Configuración de capacidad de tanque y presupuesto por activo")
public class FuelAssetConfigController {

    private final UpsertFuelAssetConfigUseCase upsertConfigUseCase;
    private final GetFuelAssetConfigUseCase getConfigUseCase;

    @PutMapping("/{assetType}/{assetId}")
    @Operation(summary = "Crear o actualizar configuración de combustible de un activo (capacidad, unidad, presupuesto)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FuelAssetConfigResponse> upsertConfig(
            @PathVariable String assetType,
            @PathVariable Long assetId,
            @RequestBody FuelAssetConfigRequest request) {
        return ResponseEntity.ok(upsertConfigUseCase.upsertConfig(assetType, assetId, request));
    }

    @GetMapping("/{assetType}/{assetId}")
    @Operation(summary = "Consultar configuración de combustible de un activo")
    @PreAuthorize("hasAnyRole('OPERARIO','ADMIN')")
    public ResponseEntity<FuelAssetConfigResponse> getConfig(
            @PathVariable String assetType,
            @PathVariable Long assetId) {
        return getConfigUseCase.getConfig(assetType, assetId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}

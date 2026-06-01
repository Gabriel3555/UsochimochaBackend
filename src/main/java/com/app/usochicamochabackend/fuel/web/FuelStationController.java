package com.app.usochicamochabackend.fuel.web;

import com.app.usochicamochabackend.exception.ResourceNotFoundException;
import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelStationEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelStationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/fuel/stations")
@RequiredArgsConstructor
@Tag(name = "Estaciones de combustible", description = "Catálogo de bombas/estaciones administrado por el administrador")
public class FuelStationController {

    private final FuelStationRepository repository;

    @GetMapping
    @Operation(summary = "Listar estaciones activas (usado por la app móvil para sincronizar)")
    @PreAuthorize("hasAnyRole('OPERARIO','ADMIN','ACEITE')")
    public ResponseEntity<List<Map<String, Object>>> listActive() {
        List<Map<String, Object>> result = repository.findByStatusTrueOrderByNameAsc()
                .stream()
                .map(s -> Map.<String, Object>of("id", s.getId(), "name", s.getName()))
                .toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping
    @Operation(summary = "Crear estación de combustible")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FuelStationEntity> create(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        FuelStationEntity saved = repository.save(FuelStationEntity.builder().name(name.trim()).build());
        return ResponseEntity.created(URI.create("/api/v1/fuel/stations/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar nombre de una estación")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FuelStationEntity> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        FuelStationEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estación no encontrada: " + id));
        String name = body.get("name");
        if (name != null && !name.isBlank()) {
            entity.setName(name.trim());
        }
        return ResponseEntity.ok(repository.save(entity));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar estación (soft delete)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        FuelStationEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estación no encontrada: " + id));
        entity.setStatus(false);
        repository.save(entity);
        return ResponseEntity.noContent().build();
    }
}

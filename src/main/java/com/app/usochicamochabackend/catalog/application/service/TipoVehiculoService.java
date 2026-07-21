package com.app.usochicamochabackend.catalog.application.service;

import com.app.usochicamochabackend.catalog.application.dto.CatalogDTO;
import com.app.usochicamochabackend.catalog.infrastructure.entity.TipoVehiculoEntity;
import com.app.usochicamochabackend.catalog.infrastructure.repository.TipoVehiculoRepository;
import com.app.usochicamochabackend.common.text.InputTextNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TipoVehiculoService {
    private final TipoVehiculoRepository repository;

    private static String requireUpperName(String raw) {
        String n = InputTextNormalizer.normalizeUpperToken(raw);
        if (n == null || n.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre es obligatorio");
        }
        return n;
    }

    public List<CatalogDTO> findAll() {
        return repository.findAll().stream()
                .map(t -> new CatalogDTO(t.getId(), t.getNombreTipo(), t.getActivo()))
                .toList();
    }

    public CatalogDTO create(CatalogDTO dto) {
        String nombre = requireUpperName(dto.name());
        if (repository.findByNombreTipoIgnoreCase(nombre).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un tipo de vehículo con ese nombre.");
        }
        TipoVehiculoEntity entity = TipoVehiculoEntity.builder()
                .nombreTipo(nombre)
                .activo(true)
                .build();
        TipoVehiculoEntity saved = repository.save(entity);
        return new CatalogDTO(saved.getId(), saved.getNombreTipo(), saved.getActivo());
    }

    public CatalogDTO update(Integer id, CatalogDTO dto) {
        TipoVehiculoEntity entity = repository.findById(id).orElseThrow();
        String nombre = requireUpperName(dto.name());
        var otra = repository.findByNombreTipoIgnoreCase(nombre);
        if (otra.isPresent() && !otra.get().getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un tipo de vehículo con ese nombre.");
        }
        entity.setNombreTipo(nombre);
        entity.setActivo(dto.active());
        TipoVehiculoEntity saved = repository.save(entity);
        return new CatalogDTO(saved.getId(), saved.getNombreTipo(), saved.getActivo());
    }

    public void delete(Integer id) {
        repository.deleteById(id);
    }
}

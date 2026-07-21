package com.app.usochicamochabackend.catalog.application.service;

import com.app.usochicamochabackend.catalog.application.dto.CatalogDTO;
import com.app.usochicamochabackend.catalog.infrastructure.entity.UbicacionEntity;
import com.app.usochicamochabackend.catalog.infrastructure.repository.UbicacionRepository;
import com.app.usochicamochabackend.common.text.InputTextNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UbicacionService {
    private final UbicacionRepository repository;

    private static String requireTitleName(String raw) {
        String n = InputTextNormalizer.normalizeTitleWords(raw);
        if (n == null || n.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre es obligatorio");
        }
        return n;
    }

    public List<CatalogDTO> findAll() {
        return repository.findAll().stream()
                .map(u -> new CatalogDTO(u.getId(), u.getNombreUbicacion(), u.getActivo()))
                .toList();
    }

    public CatalogDTO create(CatalogDTO dto) {
        String nombre = requireTitleName(dto.name());
        if (repository.existsByNombreUbicacionIgnoreCase(nombre)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una ubicación con ese nombre.");
        }
        UbicacionEntity entity = UbicacionEntity.builder()
                .nombreUbicacion(nombre)
                .activo(true)
                .build();
        UbicacionEntity saved = repository.save(entity);
        return new CatalogDTO(saved.getId(), saved.getNombreUbicacion(), saved.getActivo());
    }

    public CatalogDTO update(Integer id, CatalogDTO dto) {
        UbicacionEntity entity = repository.findById(id).orElseThrow();
        String nombre = requireTitleName(dto.name());
        if (repository.existsByNombreUbicacionIgnoreCaseAndIdNot(nombre, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una ubicación con ese nombre.");
        }
        entity.setNombreUbicacion(nombre);
        entity.setActivo(dto.active());
        UbicacionEntity saved = repository.save(entity);
        return new CatalogDTO(saved.getId(), saved.getNombreUbicacion(), saved.getActivo());
    }

    public void delete(Integer id) {
        repository.deleteById(id);
    }
}

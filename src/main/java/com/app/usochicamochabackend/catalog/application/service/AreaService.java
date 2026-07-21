package com.app.usochicamochabackend.catalog.application.service;

import com.app.usochicamochabackend.catalog.application.dto.CatalogDTO;
import com.app.usochicamochabackend.catalog.infrastructure.entity.AreaEntity;
import com.app.usochicamochabackend.catalog.infrastructure.repository.AreaRepository;
import com.app.usochicamochabackend.common.text.InputTextNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AreaService {
    private final AreaRepository areaRepository;

    private static String requireTitleName(String raw) {
        String n = InputTextNormalizer.normalizeTitleWords(raw);
        if (n == null || n.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre es obligatorio");
        }
        return n;
    }

    public List<CatalogDTO> findAll() {
        return areaRepository.findAll().stream()
                .map(a -> new CatalogDTO(a.getId(), a.getNombre(), a.getActivo()))
                .toList();
    }

    public CatalogDTO create(CatalogDTO dto) {
        String nombre = requireTitleName(dto.name());
        if (areaRepository.existsByNombre(nombre)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un área con ese nombre.");
        }
        AreaEntity entity = AreaEntity.builder()
                .nombre(nombre)
                .activo(true)
                .build();
        AreaEntity saved = areaRepository.save(entity);
        return new CatalogDTO(saved.getId(), saved.getNombre(), saved.getActivo());
    }

    public CatalogDTO update(Integer id, CatalogDTO dto) {
        AreaEntity entity = areaRepository.findById(id).orElseThrow();
        String nombre = requireTitleName(dto.name());
        if (areaRepository.existsByNombreAndIdNot(nombre, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un área con ese nombre.");
        }
        entity.setNombre(nombre);
        entity.setActivo(dto.active());
        AreaEntity saved = areaRepository.save(entity);
        return new CatalogDTO(saved.getId(), saved.getNombre(), saved.getActivo());
    }

    public void delete(Integer id) {
        areaRepository.deleteById(id);
    }
}

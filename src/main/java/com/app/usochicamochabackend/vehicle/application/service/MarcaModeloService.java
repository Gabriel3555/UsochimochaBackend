package com.app.usochicamochabackend.vehicle.application.service;

import com.app.usochicamochabackend.actions.application.port.SaveActionUseCase;
import com.app.usochicamochabackend.auth.application.dto.UserPrincipal;
import com.app.usochicamochabackend.common.text.InputTextNormalizer;
import com.app.usochicamochabackend.vehicle.application.dto.MarcaModeloRequest;
import com.app.usochicamochabackend.vehicle.application.dto.MarcaModeloResponse;
import com.app.usochicamochabackend.vehicle.application.port.MarcaModeloUseCase;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.MarcaModeloEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.MarcaModeloRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MarcaModeloService implements MarcaModeloUseCase {

    private final MarcaModeloRepository repository;
    private final SaveActionUseCase saveActionUseCase;

    @Override
    public List<MarcaModeloResponse> findAll() {
        return repository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public MarcaModeloResponse create(MarcaModeloRequest request) {
        String desc = InputTextNormalizer.normalizeTitleWords(request.descripcion());
        if (desc == null || desc.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La descripción es obligatoria");
        }
        if (repository.existsByDescripcionIgnoreCase(desc)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una marca con ese nombre.");
        }
        MarcaModeloEntity entity = MarcaModeloEntity.builder()
                .descripcion(desc)
                .build();
        entity = repository.save(entity);
        registrarAccion("La marca " + entity.getDescripcion() + " ha sido creada");
        return mapToResponse(entity);
    }

    @Override
    public MarcaModeloResponse update(Integer id, MarcaModeloRequest request) {
        MarcaModeloEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Marca no encontrada"));
        
        String desc = InputTextNormalizer.normalizeTitleWords(request.descripcion());
        if (desc == null || desc.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La descripción es obligatoria");
        }
        if (repository.existsByDescripcionIgnoreCaseAndIdMarcaNot(desc, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una marca con ese nombre.");
        }
        entity.setDescripcion(desc);
        entity = repository.save(entity);
        registrarAccion("La marca " + entity.getDescripcion() + " ha sido editada");
        return mapToResponse(entity);
    }

    @Override
    public void delete(Integer id) {
        MarcaModeloEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Marca no encontrada"));
        repository.deleteById(id);
        registrarAccion("La marca " + entity.getDescripcion() + " ha sido eliminada");
    }

    // Mismo patrón que VehicleService/MachineService: incluye el usuario si hay
    // sesión autenticada, si no, registra la acción igual sin nombre de usuario.
    private void registrarAccion(String descripcionAccion) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            saveActionUseCase.save(descripcionAccion + " por " + userPrincipal.username());
        } else {
            saveActionUseCase.save(descripcionAccion);
        }
    }

    private MarcaModeloResponse mapToResponse(MarcaModeloEntity entity) {
        return new MarcaModeloResponse(
                entity.getIdMarca(),
                entity.getDescripcion()
        );
    }
}

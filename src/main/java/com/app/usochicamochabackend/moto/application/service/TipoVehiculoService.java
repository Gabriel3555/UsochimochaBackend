package com.app.usochicamochabackend.moto.application.service;

import com.app.usochicamochabackend.catalog.infrastructure.entity.TipoVehiculoEntity;
import com.app.usochicamochabackend.catalog.infrastructure.repository.TipoVehiculoRepository;
import com.app.usochicamochabackend.moto.application.dto.TipoVehiculoResponse;
import com.app.usochicamochabackend.moto.application.port.FindAllTiposVehiculoUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("motoTipoVehiculoService")
@RequiredArgsConstructor
public class TipoVehiculoService implements FindAllTiposVehiculoUseCase {

    private final TipoVehiculoRepository tipoVehiculoRepository;

    @Override
    public List<TipoVehiculoResponse> findAll() {
        return tipoVehiculoRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    private TipoVehiculoResponse mapToResponse(TipoVehiculoEntity entity) {
        return new TipoVehiculoResponse(
                entity.getId(),
                entity.getNombreTipo(),
                entity.getActivo()
        );
    }
}

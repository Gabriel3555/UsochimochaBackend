package com.app.usochicamochabackend.vehicle.application.port;

import com.app.usochicamochabackend.vehicle.application.dto.MarcaModeloRequest;
import com.app.usochicamochabackend.vehicle.application.dto.MarcaModeloResponse;

import java.util.List;

public interface MarcaModeloUseCase {
    List<MarcaModeloResponse> findAll();
    MarcaModeloResponse findById(Integer id);
    MarcaModeloResponse create(MarcaModeloRequest request);
    MarcaModeloResponse update(Integer id, MarcaModeloRequest request);
    void delete(Integer id);
}

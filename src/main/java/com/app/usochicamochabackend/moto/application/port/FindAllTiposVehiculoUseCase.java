package com.app.usochicamochabackend.moto.application.port;

import com.app.usochicamochabackend.moto.application.dto.TipoVehiculoResponse;
import java.util.List;

public interface FindAllTiposVehiculoUseCase {
    List<TipoVehiculoResponse> findAll();
}

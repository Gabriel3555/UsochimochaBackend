package com.app.usochicamochabackend.fuel.application.port;

import com.app.usochicamochabackend.fuel.application.dto.FuelMonthlyDiscountRequest;
import com.app.usochicamochabackend.fuel.application.dto.FuelMonthlyDiscountResponse;

import java.util.List;

public interface ManageFuelMonthlyDiscountUseCase {
    FuelMonthlyDiscountResponse registrar(FuelMonthlyDiscountRequest request, Long responsableId);
    List<FuelMonthlyDiscountResponse> listar();
    void eliminar(Long id);
}

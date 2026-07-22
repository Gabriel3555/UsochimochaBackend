package com.app.usochicamochabackend.fuel.application.port;

import com.app.usochicamochabackend.fuel.application.dto.FuelPurchaseRequest;
import com.app.usochicamochabackend.fuel.application.dto.FuelPurchaseResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface RegisterFuelPurchaseUseCase {
    FuelPurchaseResponse registrar(FuelPurchaseRequest request, MultipartFile factura, Long responsableId);
    Page<FuelPurchaseResponse> listar(Pageable pageable);
}

package com.app.usochicamochabackend.fuel.web;

import com.app.usochicamochabackend.fuel.application.dto.FuelTypesResponse;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelTypesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fuel")
@RequiredArgsConstructor
public class FuelTypeController {

    private final FuelTypesRepository fuelTypesRepository;

    @GetMapping("/types")
    public ResponseEntity<List<FuelTypesResponse>> getTypes(){
        List<FuelTypesResponse> response = fuelTypesRepository.findAll().stream()
                .map(entity -> new FuelTypesResponse(
                        entity.getId(),
                        entity.getCodigo(),
                        entity.getNombre(),
                        entity.getActivo(),
                        entity.getUnidadMedida()
                ))
                .toList();

        return ResponseEntity.ok(response);
    }
}

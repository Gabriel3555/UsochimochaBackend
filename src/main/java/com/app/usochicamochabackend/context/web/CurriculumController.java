package com.app.usochicamochabackend.context.web;

import com.app.usochicamochabackend.context.application.dto.MachineCurriculumDTO;
import com.app.usochicamochabackend.context.application.dto.VehicleCurriculumDTO;
import com.app.usochicamochabackend.context.application.port.GetMachineCurriculumUseCase;
import com.app.usochicamochabackend.context.application.port.GetVehicleCurriculumUseCase;
import com.app.usochicamochabackend.exception.ResourceNotFoundException;
import com.app.usochicamochabackend.machine.application.port.FindAllMachinesUseCase;
import com.app.usochicamochabackend.moto.application.port.MotoCRUDUseCase;
import com.app.usochicamochabackend.update.application.service.ExcelGenerationService;
import com.app.usochicamochabackend.vehicle.application.port.VehicleUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/curriculum")
@RequiredArgsConstructor
@Tag(name = "Curriculum", description = "Endpoints for managing curriculum")
public class CurriculumController {

    private final GetMachineCurriculumUseCase getMachineCurriculumUseCase;
    private final GetVehicleCurriculumUseCase getVehicleCurriculumUseCase;
    private final FindAllMachinesUseCase findAllMachinesUseCase;
    private final VehicleUseCase vehicleUseCase;
    private final MotoCRUDUseCase motoCRUDUseCase;
    private final ExcelGenerationService excelGenerationService;

    @Operation(summary = "Get machine curriculum", description = "Returns the curriculum of a machine with inspections, orders and results")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Curriculum found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MachineCurriculumDTO.class))),
            @ApiResponse(responseCode = "404", description = "Machine not found",
                    content = @Content)
    })
    @GetMapping("/{machineId}")
    public ResponseEntity<MachineCurriculumDTO> getMachineCurriculum(
            @PathVariable Long machineId) {
        MachineCurriculumDTO curriculum = getMachineCurriculumUseCase.getMachineCurriculum(machineId);
        return ResponseEntity.ok(curriculum);
    }

    @Operation(summary = "Get vehicle curriculum", description = "Returns the curriculum of a vehicle/motorcycle with work orders and results")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Curriculum found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = VehicleCurriculumDTO.class))),
            @ApiResponse(responseCode = "404", description = "Vehicle not found",
                    content = @Content)
    })
    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<VehicleCurriculumDTO> getVehicleCurriculum(@PathVariable Integer vehicleId) {
        return ResponseEntity.ok(getVehicleCurriculumUseCase.getVehicleCurriculum(vehicleId));
    }

    @Operation(summary = "Export machine curricula to Excel", description = "Exports the curriculum of all machines to an Excel file with one sheet per machine")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Excel file generated successfully",
                    content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
            @ApiResponse(responseCode = "500", description = "Error generating Excel file",
                    content = @Content)
    })
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportMachineCurricula() throws IOException {
        List<MachineCurriculumDTO> curricula = findAllMachinesUseCase.findAllMachines().stream()
                .filter(machine -> {
                    try {
                        getMachineCurriculumUseCase.getMachineCurriculum(machine.id());
                        return true;
                    } catch (ResourceNotFoundException e) {
                        return false;
                    }
                })
                .map(machine -> getMachineCurriculumUseCase.getMachineCurriculum(machine.id()))
                .toList();

        byte[] excelData = excelGenerationService.generateMachineCurriculumExcel(curricula);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "machine_curricula.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }

    @Operation(summary = "Export vehicle curricula to Excel", description = "Exports the curriculum of all cars (non-moto) to an Excel file with one sheet per vehicle")
    @GetMapping("/export/vehicles")
    public ResponseEntity<byte[]> exportVehicleCurricula() throws IOException {
        List<VehicleCurriculumDTO> curricula = vehicleUseCase.findAllVehicles().stream()
                .filter(v -> !String.valueOf(v.tipoVehiculo()).toLowerCase(Locale.ROOT).contains("moto"))
                .filter(v -> {
                    try {
                        getVehicleCurriculumUseCase.getVehicleCurriculum(v.id());
                        return true;
                    } catch (ResourceNotFoundException e) {
                        return false;
                    }
                })
                .map(v -> getVehicleCurriculumUseCase.getVehicleCurriculum(v.id()))
                .toList();

        byte[] excelData = excelGenerationService.generateVehicleCurriculumExcel(curricula);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "vehiculos_curriculum.xlsx");

        return ResponseEntity.ok().headers(headers).body(excelData);
    }

    @Operation(summary = "Export moto curricula to Excel", description = "Exports the curriculum of all motorcycles to an Excel file with one sheet per moto")
    @GetMapping("/export/motos")
    public ResponseEntity<byte[]> exportMotoCurricula() throws IOException {
        List<VehicleCurriculumDTO> curricula = motoCRUDUseCase.findAllMotos().stream()
                .filter(v -> {
                    try {
                        getVehicleCurriculumUseCase.getVehicleCurriculum(v.id());
                        return true;
                    } catch (ResourceNotFoundException e) {
                        return false;
                    }
                })
                .map(v -> getVehicleCurriculumUseCase.getVehicleCurriculum(v.id()))
                .toList();

        byte[] excelData = excelGenerationService.generateVehicleCurriculumExcel(curricula);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "motos_curriculum.xlsx");

        return ResponseEntity.ok().headers(headers).body(excelData);
    }

}

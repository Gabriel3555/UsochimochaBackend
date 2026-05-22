package com.app.usochicamochabackend.review.web;

import com.app.usochicamochabackend.review.application.dto.ImageDTO;
import com.app.usochicamochabackend.review.application.dto.InspectionDTO;
import com.app.usochicamochabackend.review.application.dto.InspectionFormRequest;
import com.app.usochicamochabackend.review.application.dto.InspectionFormResponse;
import com.app.usochicamochabackend.review.application.port.*;
import com.app.usochicamochabackend.update.application.service.ExcelGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/inspection")
@RequiredArgsConstructor
@Tag(name = "Inspection", description = "Endpoints for managing inspections")
public class InspectionController {

    private final CreateInspectionOnlyDataUseCase createInspectionOnlyDataUseCase;
    private final GetAllInspectionsWithoutImagesUseCase getAllInspectionsWithoutImagesUseCase;
    private final GetInspectionByIdUseCase getInspectionByIdUseCase;
    private final GetInspectionImagesUseCase  getInspectionImagesUseCase;
    private final SaveInspectionImageUseCase  saveInspectionImageUseCase;
    private final GetAllInspectionsForExportUseCase getAllInspectionsForExportUseCase;
    private final UpdateInspectionHourMeterUseCase updateInspectionHourMeterUseCase;
    private final ExcelGenerationService excelGenerationService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Create a new inspection",
            description = "This endpoint allows creating an inspection using only JSON data (without images)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Inspection successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<InspectionFormResponse> createInspection(@RequestBody InspectionFormRequest request) throws URISyntaxException {
        InspectionFormResponse saved = createInspectionOnlyDataUseCase.createInspectionOnlyData(request);

        return ResponseEntity
                .created(new URI("/api/v1/inspection/" + saved.id()))
                .body(saved);
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload an inspection image",
            description = "This endpoint allows uploading a single image for an existing inspection. " +
                    "Requires the inspection ID and UUID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Image successfully uploaded"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Inspection not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ImageDTO> uploadInspectionImage(
            @Parameter(description = "Inspection ID", required = true)
            @PathVariable("id") Long inspectionId,
            @Parameter(description = "Image to upload", required = true)
            @RequestPart("imagen") MultipartFile imagen
    ) throws IOException, URISyntaxException {

        ImageDTO savedImage = saveInspectionImageUseCase.saveInspectionImage(inspectionId, imagen);

        return ResponseEntity
                .created(new URI("/api/v1/inspection/" + inspectionId + "/image/" + savedImage.id()))
                .body(savedImage);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get an inspection by ID",
            description = "Returns all the information of an inspection, including the associated user and machine."
    )
    public ResponseEntity<InspectionDTO> getInspectionById(@PathVariable Long id) {
        InspectionDTO inspection = getInspectionByIdUseCase.getInspectionById(id);
        return ResponseEntity.ok(inspection);
    }

    @GetMapping("/{inspectionId}/images")
    @Operation(
            summary = "Get inspection images",
            description = "Returns all images associated with an inspection."
    )
    public ResponseEntity<List<ImageDTO>> getInspectionImages(@PathVariable Long inspectionId) {
        return ResponseEntity.ok(getInspectionImagesUseCase.getAllImagesByInspectionId(inspectionId));
    }

    @GetMapping
    @Operation(
            summary = "Get all inspections (paginated)",
            description = "Returns all inspections without including images, with pagination."
    )
    public ResponseEntity<Page<InspectionFormResponse>> getAllInspections(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<InspectionFormResponse> inspections = getAllInspectionsWithoutImagesUseCase.getAllInspectionsWithoutImages(pageable);
        return ResponseEntity.ok(inspections);
    }

    @PatchMapping("/machine/{machineId}/hour-meter")
    @Operation(
            summary = "Correct the hourmeter of the latest inspection for a machine",
            description = "Updates the hourMeter field of the most recent inspection for the given machine."
    )
    public ResponseEntity<Void> updateHourMeter(
            @PathVariable Long machineId,
            @RequestBody Double newHourMeter
    ) {
        updateInspectionHourMeterUseCase.updateHourMeter(machineId, newHourMeter);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export")
    @Operation(
            summary = "Export all inspections to Excel",
            description = "Generates and downloads an Excel file with all inspections data."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Excel file generated successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<byte[]> exportInspectionsToExcel() throws IOException {
        List<com.app.usochicamochabackend.review.infrastructure.entity.InspectionEntity> inspections = getAllInspectionsForExportUseCase.getAllInspectionsForExport();
        byte[] excelData = excelGenerationService.generateInspectionsExcel(inspections);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "inspecciones.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }

}

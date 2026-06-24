package com.app.usochicamochabackend.review.web;

import com.app.usochicamochabackend.auth.utils.JwtUtils;
import com.app.usochicamochabackend.review.application.dto.InspectionFormRequest;
import com.app.usochicamochabackend.review.application.port.*;
import com.app.usochicamochabackend.update.application.service.ExcelGenerationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InspectionController.class)
@AutoConfigureMockMvc
class InspectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateInspectionOnlyDataUseCase createInspectionOnlyDataUseCase;

    @MockBean
    private GetAllInspectionsWithoutImagesUseCase getAllInspectionsWithoutImagesUseCase;

    @MockBean
    private GetInspectionByIdUseCase getInspectionByIdUseCase;

    @MockBean
    private GetInspectionImagesUseCase getInspectionImagesUseCase;

    @MockBean
    private SaveInspectionImageUseCase saveInspectionImageUseCase;

    @MockBean
    private GetAllInspectionsForExportUseCase getAllInspectionsForExportUseCase;

    @MockBean
    private UpdateInspectionHourMeterUseCase updateInspectionHourMeterUseCase;

    @MockBean
    private ExcelGenerationService excelGenerationService;

    @MockBean
    private JwtUtils jwtUtils;

    @Test
    @DisplayName("GET /api/v1/inspection: retorna página de inspecciones")
    @WithMockUser
    void getAllInspections_RetornaPagina() throws Exception {
        when(getAllInspectionsWithoutImagesUseCase.getAllInspectionsWithoutImages(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/inspection"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/inspection/{id}: inspección existente → 200")
    @WithMockUser
    void getInspectionById_Existente_Devuelve200() throws Exception {
        when(getInspectionByIdUseCase.getInspectionById(1L)).thenReturn(null);

        mockMvc.perform(get("/api/v1/inspection/1"))
                .andExpect(status().isOk());
    }
}

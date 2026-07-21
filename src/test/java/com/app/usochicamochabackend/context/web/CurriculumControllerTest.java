package com.app.usochicamochabackend.context.web;

import com.app.usochicamochabackend.auth.utils.JwtUtils;
import com.app.usochicamochabackend.context.application.port.GetMachineCurriculumUseCase;
import com.app.usochicamochabackend.context.application.port.GetVehicleCurriculumUseCase;
import com.app.usochicamochabackend.machine.application.port.FindAllMachinesUseCase;
import com.app.usochicamochabackend.moto.application.port.MotoCRUDUseCase;
import com.app.usochicamochabackend.update.application.service.ExcelGenerationService;
import com.app.usochicamochabackend.vehicle.application.port.VehicleUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CurriculumController.class)
@AutoConfigureMockMvc
class CurriculumControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetMachineCurriculumUseCase getMachineCurriculumUseCase;

    @MockBean
    private GetVehicleCurriculumUseCase getVehicleCurriculumUseCase;

    @MockBean
    private FindAllMachinesUseCase findAllMachinesUseCase;

    @MockBean
    private VehicleUseCase vehicleUseCase;

    @MockBean
    private MotoCRUDUseCase motoCRUDUseCase;

    @MockBean
    private ExcelGenerationService excelGenerationService;

    @MockBean
    private JwtUtils jwtUtils;

    @Test
    @DisplayName("GET /api/v1/curriculum/{machineId}: curriculum de máquina → 200")
    @WithMockUser
    void getMachineCurriculum_RetornaOk() throws Exception {
        when(getMachineCurriculumUseCase.getMachineCurriculum(1L)).thenReturn(null);

        mockMvc.perform(get("/api/v1/curriculum/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/curriculum/vehicle/{vehicleId}: curriculum de vehículo → 200")
    @WithMockUser
    void getVehicleCurriculum_RetornaOk() throws Exception {
        when(getVehicleCurriculumUseCase.getVehicleCurriculum(1)).thenReturn(null);

        mockMvc.perform(get("/api/v1/curriculum/vehicle/1"))
                .andExpect(status().isOk());
    }
}

package com.app.usochicamochabackend.actions.web;

import com.app.usochicamochabackend.actions.application.port.GetAllActionsByUserIdUseCase;
import com.app.usochicamochabackend.actions.application.port.GetAllActionsUseCase;
import com.app.usochicamochabackend.actions.infrastructure.entity.ActionEntity;
import com.app.usochicamochabackend.auth.utils.JwtUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ActionsController.class)
@AutoConfigureMockMvc
class ActionsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetAllActionsUseCase getAllActionsUseCase;

    @MockBean
    private GetAllActionsByUserIdUseCase getAllActionsByUserIdUseCase;

    @MockBean
    private JwtUtils jwtUtils;

    @Test
    @DisplayName("GET /api/v1/actions: retorna página de acciones")
    @WithMockUser(roles = "ADMIN")
    void getAllActions_RetornaPagina() throws Exception {
        when(getAllActionsUseCase.getAllActions(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new ActionEntity())));

        mockMvc.perform(get("/api/actions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/actions/user/{userId}: retorna acciones del usuario")
    @WithMockUser(roles = "ADMIN")
    void getActionsByUserId_RetornaPagina() throws Exception {
        when(getAllActionsByUserIdUseCase.getAllActionsByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new ActionEntity())));

        mockMvc.perform(get("/api/actions/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }
}

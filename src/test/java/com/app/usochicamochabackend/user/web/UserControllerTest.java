package com.app.usochicamochabackend.user.web;

import com.app.usochicamochabackend.auth.utils.JwtUtils;
import com.app.usochicamochabackend.user.application.dto.UserResponse;
import com.app.usochicamochabackend.user.application.dto.UsersResponse;
import com.app.usochicamochabackend.user.application.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtUtils jwtUtils;

    @Test
    @DisplayName("GET /api/v1/user: retorna lista de usuarios")
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_RetornaLista() throws Exception {
        when(userService.findAllUsers()).thenReturn(new UsersResponse(List.of()));

        mockMvc.perform(get("/api/v1/user"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/user/{id}: usuario existente → 200")
    @WithMockUser(roles = "ADMIN")
    void getUserById_RetornaOk() throws Exception {
        when(userService.findUserById(1L)).thenReturn(null);

        mockMvc.perform(get("/api/v1/user/1"))
                .andExpect(status().isOk());
    }
}

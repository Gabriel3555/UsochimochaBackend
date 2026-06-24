package com.app.usochicamochabackend.actions.application.service;

import com.app.usochicamochabackend.actions.infrastructure.entity.ActionEntity;
import com.app.usochicamochabackend.actions.infrastructure.repository.ActionRepository;
import com.app.usochicamochabackend.auth.application.dto.UserPrincipal;
import com.app.usochicamochabackend.auth.infrastructure.entity.UserEntity;
import com.app.usochicamochabackend.auth.infrastructure.repository.UserRepositoryJpa;
import com.app.usochicamochabackend.notifications.application.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActionsServiceTest {

    @Mock
    private ActionRepository actionRepository;

    @Mock
    private UserRepositoryJpa userRepositoryJpa;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ActionsService actionsService;

    private UserEntity mockUserEntity;

    @BeforeEach
    void setupSecurityContext() {
        UserPrincipal principal = new UserPrincipal(1L, "testuser");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        mockUserEntity = new UserEntity();
        mockUserEntity.setId(1L);
        mockUserEntity.setUsername("testuser");
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("save: guarda una acción para el usuario autenticado")
    void save_GuardaAccionParaUsuarioAutenticado() {
        when(userRepositoryJpa.getUserEntityById(1L)).thenReturn(mockUserEntity);
        when(actionRepository.save(any())).thenReturn(new ActionEntity());

        actionsService.save("Acción de prueba");

        verify(actionRepository).save(any(ActionEntity.class));
    }

    @Test
    @DisplayName("getAllActions: registra acción de observación y retorna página")
    void getAllActions_RetornaPagina() {
        ActionEntity entity = new ActionEntity();
        Page<ActionEntity> page = new PageImpl<>(List.of(entity));

        when(userRepositoryJpa.getUserEntityById(1L)).thenReturn(mockUserEntity);
        when(actionRepository.save(any())).thenReturn(entity);
        when(actionRepository.findAll(any(PageRequest.class))).thenReturn(page);

        var result = actionsService.getAllActions(PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("getAllActionsByUserId: retorna página filtrada por usuario")
    void getAllActionsByUserId_RetornaPaginaFiltrada() {
        ActionEntity entity = new ActionEntity();
        Page<ActionEntity> page = new PageImpl<>(List.of(entity));

        when(userRepositoryJpa.getUserEntityById(anyLong())).thenReturn(mockUserEntity);
        when(actionRepository.save(any())).thenReturn(entity);
        when(actionRepository.findByUserId(eq(5L), any(PageRequest.class))).thenReturn(page);

        var result = actionsService.getAllActionsByUserId(5L, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }
}

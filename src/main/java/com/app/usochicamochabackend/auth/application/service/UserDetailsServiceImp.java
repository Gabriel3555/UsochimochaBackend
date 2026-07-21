package com.app.usochicamochabackend.auth.application.service;

import com.app.usochicamochabackend.actions.application.port.SaveActionUseCase;
import com.app.usochicamochabackend.auth.application.dto.*;
import com.app.usochicamochabackend.auth.application.port.AuthenticateUseCase;
import com.app.usochicamochabackend.auth.application.port.LoginUseCase;
import com.app.usochicamochabackend.auth.application.port.RefreshTokenUseCase;
import com.app.usochicamochabackend.auth.application.port.SearchUsernameUseCase;
import com.app.usochicamochabackend.auth.infrastructure.entity.UserEntity;
import com.app.usochicamochabackend.auth.infrastructure.repository.UserRepositoryJpa;
import com.app.usochicamochabackend.auth.utils.JwtUtils;
import com.app.usochicamochabackend.notifications.application.NotificationService;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImp
        implements LoginUseCase, AuthenticateUseCase, SearchUsernameUseCase, RefreshTokenUseCase, UserDetailsService {

    private final UserRepositoryJpa userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final SaveActionUseCase saveActionUseCase;
    private final NotificationService notificationService;

    @Override
    public AuthResponse login(AuthRequest authLoginRequest) {
        String username = authLoginRequest.username();
        String password = authLoginRequest.password();

        UserEntity userEntity = userRepository.findActiveByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("user not found!"));

        Long id = userEntity.getId();

        Authentication authentication = this.authenticate(new UserPrincipal(id, username), password);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwtToken = jwtUtils.createToken(authentication);
        String refreshToken = jwtUtils.createRefreshToken(authentication);

        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();

        this.saveLoginAuditAsync(userPrincipal.username());

        return new AuthResponse(userEntity.getId(), username, userEntity.getFullName(), userEntity.getRole(),
                "logged successfully!", jwtToken, refreshToken, true);
    }

    @Override
    public Authentication authenticate(UserPrincipal userPrincipal, String password) {
        UserDetails userDetails = this.searchUserDetails(userPrincipal.username());

        if (userDetails == null) {
            throw new BadCredentialsException("Invalid username or password");
        }

        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new BadCredentialsException("invalid password");
        }

        return new UsernamePasswordAuthenticationToken(userPrincipal, null, userDetails.getAuthorities());
    }

    @Override
    public UserDetails searchUserDetails(String username) {
        UserEntity userEntity = userRepository.findActiveByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("user not found"));

        GrantedAuthority role = new SimpleGrantedAuthority("ROLE_".concat(userEntity.getRole()));

        return new User(username, userEntity.getPassword(), Set.of(role));
    }

    @Override
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        try {
            DecodedJWT decodedJWT = jwtUtils.verifyToken(request.refreshToken());
            String username = jwtUtils.extractUsername(decodedJWT);

            UserEntity userEntity = userRepository.findActiveByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("user not found"));

            GrantedAuthority role = new SimpleGrantedAuthority("ROLE_".concat(userEntity.getRole()));

            UserPrincipal userPrincipal = new UserPrincipal(userEntity.getId(), username);
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userPrincipal, null, Set.of(role));

            String newAccessToken = jwtUtils.createToken(authentication);

            return new RefreshTokenResponse(newAccessToken);

        } catch (Exception e) {
            throw new BadCredentialsException("Refresh token inválido o expirado");
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return this.searchUserDetails(username);
    }

    @Async
    protected void saveLoginAuditAsync(String username) {
        try {
            saveActionUseCase.save("El usuario " + username +
                    " ha iniciado sesion el dia " + LocalDateTime.now().toLocalDate() + " a las "
                    + LocalDateTime.now().toLocalTime());
        } catch (Exception e) {
            System.err.println("Error guardando auditoría de login para " + username + ": " + e.getMessage());
        }
    }
}

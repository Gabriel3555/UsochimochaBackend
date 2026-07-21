package com.app.usochicamochabackend.auth.security.filter;

import com.app.usochicamochabackend.auth.application.dto.UserPrincipal;
import com.app.usochicamochabackend.auth.utils.JwtUtils;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.logging.Logger;

public class JwtTokenValidator extends OncePerRequestFilter {

    private static final Logger logger = Logger.getLogger(JwtTokenValidator.class.getName());
    private final JwtUtils jwtUtils;

    public JwtTokenValidator(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            try {
                logger.info("🔐 [JWT] Validando token: " + token.substring(0, Math.min(50, token.length())) + "...");
                DecodedJWT decodedJWT = jwtUtils.verifyToken(token);
                logger.info("✅ [JWT] Token válido para usuario: " + decodedJWT.getSubject());
                Long userId = decodedJWT.getClaim("userId").asLong();
                String username = jwtUtils.extractUsername(decodedJWT);
                String roleString = jwtUtils.extractSpecificClaim(decodedJWT, "role").asString();
                GrantedAuthority role = new SimpleGrantedAuthority(roleString);

                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                new UserPrincipal(userId, username),
                                null,
                                Set.of(role)
                        )
                );

            } catch (Exception ex) {
                // 🔥 Captura error del token inválido/expirado
                logger.warning("❌ [JWT] Token inválido: " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Invalid or expired token\", \"cause\": \"" + ex.getMessage() + "\"}");
                return; // 🚨 IMPORTANTE: no seguir el chain
            }
        }

        filterChain.doFilter(request, response);
    }
}
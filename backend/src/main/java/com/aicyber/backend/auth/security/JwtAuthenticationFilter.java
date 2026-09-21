package com.aicyber.backend.auth.security;

import com.aicyber.backend.auth.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwtService.parse(header.substring(7));
                UUID userId = UUID.fromString(claims.getSubject());
                Integer tokenAuthVersion = claims.get("authVersion", Integer.class);
                int expectedAuthVersion = tokenAuthVersion == null ? 1 : tokenAuthVersion;
                var user = userRepository.findById(userId)
                        .filter(account -> "ACTIVE".equals(account.status()))
                        .filter(account -> account.authVersion() == expectedAuthVersion)
                        .orElseThrow(() -> new IllegalArgumentException("Authentication token is no longer valid"));
                var authentication = new UsernamePasswordAuthenticationToken(
                        userId.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_" + user.role())
                ));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (RuntimeException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}

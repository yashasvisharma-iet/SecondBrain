package com.example.demo.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Configuration
public class SecurityConfig {

    @Value("${app.frontend-google-callback-url:http://localhost:5173/auth/google/callback}")
    private String frontendGoogleCallbackUrl;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/google-docs/list", "/api/google-docs/ingest-selected").authenticated()
                        .anyRequest().permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(this::handleGoogleAuthSuccess)
                        .failureHandler(this::handleGoogleAuthFailure)
                )
                .logout(Customizer.withDefaults());

        return http.build();
    }

    private void handleGoogleAuthSuccess(HttpServletRequest request,
                                         HttpServletResponse response,
                                         org.springframework.security.core.Authentication authentication) throws java.io.IOException {
        response.sendRedirect(frontendGoogleCallbackUrl);
    }

    private void handleGoogleAuthFailure(HttpServletRequest request,
                                         HttpServletResponse response,
                                         AuthenticationException exception) throws java.io.IOException {
        String encodedErrorMessage = URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8);
        response.sendRedirect(frontendGoogleCallbackUrl + "?error=" + encodedErrorMessage);
    }
}

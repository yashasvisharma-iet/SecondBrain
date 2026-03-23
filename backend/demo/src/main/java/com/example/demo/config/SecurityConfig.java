package com.example.demo.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

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
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/me", "/api/graph/**", "/api/notion/**", "/api/google-docs/**", "/api/oauth/notion/callback")
                        .authenticated()
                        .anyRequest().permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(this::handleGoogleAuthSuccess)
                        .failureHandler(this::handleGoogleAuthFailure)
                )
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                (request, response, exception) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED),
                                new AntPathRequestMatcher("/api/**")
                        )
                )
                .logout(logout -> logout.logoutSuccessHandler((request, response, authentication) -> response.setStatus(HttpServletResponse.SC_NO_CONTENT)))
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    private void handleGoogleAuthSuccess(HttpServletRequest request,
                                         HttpServletResponse response,
                                         org.springframework.security.core.Authentication authentication) throws java.io.IOException {
        String userId = authentication.getName();
        log.info("[AUTH SUCCESS] userId={} redirectingTo={}", userId, frontendGoogleCallbackUrl);
        response.sendRedirect(frontendGoogleCallbackUrl);
    }

    private void handleGoogleAuthFailure(HttpServletRequest request,
                                         HttpServletResponse response,
                                         org.springframework.security.core.AuthenticationException exception) throws java.io.IOException {
        String encodedErrorMessage = URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8);
        log.error("[AUTH FAILURE] error={} redirectingTo={}", encodedErrorMessage, frontendGoogleCallbackUrl);
        response.sendRedirect(frontendGoogleCallbackUrl + "?error=" + encodedErrorMessage);
    }
}

package com.example.demo.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig();
        ReflectionTestUtils.setField(securityConfig, "frontendGoogleCallbackUrl", "http://localhost:5173/auth/google/callback");
    }

    @Test
    void successHandlerRedirectsToFrontendCallback() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        Authentication authentication = new UsernamePasswordAuthenticationToken("google-user", "n/a", List.of());

        invokeHandler("handleGoogleAuthSuccess", request, response, authentication);

        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:5173/auth/google/callback");
    }

    @Test
    void failureHandlerRedirectsToFrontendCallbackWithEncodedError() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2AuthenticationException exception = new OAuth2AuthenticationException(new OAuth2Error("invalid_token"), "Bad token");

        invokeHandler("handleGoogleAuthFailure", request, response, exception);

        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:5173/auth/google/callback?error=Bad+token");
    }

    private void invokeHandler(String methodName, HttpServletRequest request, HttpServletResponse response, Object thirdArg) throws Exception {
        Class<?> thirdType = thirdArg instanceof Authentication
                ? Authentication.class
                : org.springframework.security.core.AuthenticationException.class;
        Method method = SecurityConfig.class.getDeclaredMethod(methodName, HttpServletRequest.class, HttpServletResponse.class, thirdType);
        method.setAccessible(true);
        method.invoke(securityConfig, request, response, thirdArg);
    }
}

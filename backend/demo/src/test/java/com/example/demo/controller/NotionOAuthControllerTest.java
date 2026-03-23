package com.example.demo.controller;

import com.example.demo.entity.AppUser;
import com.example.demo.service.NotionIngestionService;
import com.example.demo.service.NotionOAuthService;
import com.example.demo.service.auth.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotionOAuthControllerTest {

    @Mock
    private NotionOAuthService notionOAuthService;
    @Mock
    private NotionIngestionService ingestionService;
    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private NotionOAuthController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    void rejectsCallbackWithoutCode() throws Exception {
        mockMvc.perform(post("/api/oauth/notion/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exchangesCodeAndIngestsSpecificPageWhenProvided() throws Exception {
        OAuth2User principal = principal();
        AppUser user = appUser(40L);
        when(currentUserService.requireUser(principal)).thenReturn(user);
        when(notionOAuthService.exchangeCode("code-123", user)).thenReturn("workspace-1");

        mockMvc.perform(post("/api/oauth/notion/callback")
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, "n/a", principal.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"code-123\",\"pageId\":\"page-7\"}"))
                .andExpect(status().isOk());

        verify(ingestionService).ingestPage(user, "workspace-1", "page-7");
    }

    @Test
    void fallsBackToRecentPageIngestionWhenPageIdMissing() throws Exception {
        OAuth2User principal = principal();
        AppUser user = appUser(40L);
        when(currentUserService.requireUser(principal)).thenReturn(user);
        when(notionOAuthService.exchangeCode("code-123", user)).thenReturn("workspace-1");

        mockMvc.perform(post("/api/oauth/notion/callback")
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, "n/a", principal.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"code-123\"}"))
                .andExpect(status().isOk());

        verify(ingestionService).ingestRecentPages(user, "workspace-1", 5);
    }

    @Test
    void stillReturnsOkWhenPostOAuthIngestionFails() throws Exception {
        OAuth2User principal = principal();
        AppUser user = appUser(40L);
        when(currentUserService.requireUser(principal)).thenReturn(user);
        when(notionOAuthService.exchangeCode("code-123", user)).thenReturn("workspace-1");
        doThrow(new IllegalStateException("boom")).when(ingestionService).ingestPage(user, "workspace-1", "page-7");

        mockMvc.perform(post("/api/oauth/notion/callback")
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, "n/a", principal.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"code-123\",\"pageId\":\"page-7\"}"))
                .andExpect(status().isOk());
    }

    private OAuth2User principal() {
        return new DefaultOAuth2User(List.of(() -> "ROLE_USER"), Map.of("sub", "sub-123"), "sub");
    }

    private AppUser appUser(Long id) {
        AppUser user = new AppUser("alice@example.com", "Alice", null, "google", "sub-123");
        try {
            var field = AppUser.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return user;
    }
}

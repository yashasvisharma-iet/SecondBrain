package com.example.demo.controller;

import com.example.demo.entity.AppUser;
import com.example.demo.repository.NotionTokenRepository;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private NotionTokenRepository notionTokenRepository;

    @InjectMocks
    private UserController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    void returnsCurrentUserDetails() throws Exception {
        OAuth2User principal = principal();
        AppUser user = new AppUser("alice@example.com", "Alice", "https://avatar", "google", "sub-123");
        setId(user, 5L);
        when(currentUserService.requireUser(any())).thenReturn(user);
        when(notionTokenRepository.existsByAppUserId(5L)).thenReturn(true);

        mockMvc.perform(get("/api/me")
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, "n/a", principal.getAuthorities())))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5L))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.googleConnected").value(true))
                .andExpect(jsonPath("$.notionConnected").value(true));
    }

    private OAuth2User principal() {
        return new DefaultOAuth2User(List.of(() -> "ROLE_USER"), Map.of("sub", "sub-123"), "sub");
    }

    private void setId(AppUser user, Long id) {
        try {
            var field = AppUser.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}

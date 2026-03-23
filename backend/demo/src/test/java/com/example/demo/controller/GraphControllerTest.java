package com.example.demo.controller;

import com.example.demo.dto.AgentQueryResponse;
import com.example.demo.dto.GraphDataDto;
import com.example.demo.dto.GraphEdgeDto;
import com.example.demo.dto.GraphNodeDto;
import com.example.demo.entity.AppUser;
import com.example.demo.service.GraphService;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GraphControllerTest {

    @Mock
    private GraphService graphService;
    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private GraphController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    void returnsFeedGraphForAuthenticatedUser() throws Exception {
        OAuth2User principal = principal();
        AppUser user = appUser(12L);
        when(currentUserService.requireUser(principal)).thenReturn(user);
        when(graphService.getFeedGraph(12L)).thenReturn(new GraphDataDto(
                List.of(new GraphNodeDto("page-1", "Page 1", "note", "Life")),
                List.of(new GraphEdgeDto("page-1", "page-2", 0.91))
        ));

        mockMvc.perform(get("/api/graph/feed")
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, "n/a", principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes[0].id").value("page-1"))
                .andExpect(jsonPath("$.edges[0].score").value(0.91));
    }

    @Test
    void asksAgentUsingRequestBodyQuery() throws Exception {
        OAuth2User principal = principal();
        AppUser user = appUser(12L);
        when(currentUserService.requireUser(principal)).thenReturn(user);
        when(graphService.answerFromDatabase(12L, "Where are my notes?"))
                .thenReturn(new AgentQueryResponse("Found them", List.of()));

        mockMvc.perform(post("/api/graph/ask")
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, "n/a", principal.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"Where are my notes?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Found them"));
    }

    @Test
    void summarizesRequestedPage() throws Exception {
        OAuth2User principal = principal();
        AppUser user = appUser(12L);
        when(currentUserService.requireUser(principal)).thenReturn(user);
        when(graphService.summarizePage(12L, "page-1")).thenReturn("Short summary");

        mockMvc.perform(post("/api/graph/summarize")
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, "n/a", principal.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pageId\":\"page-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("Short summary"));
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

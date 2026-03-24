package com.example.demo.controller;

import com.example.demo.entity.AppUser;
import com.example.demo.entity.NotionPageContent;
import com.example.demo.repository.NotionPageContentRepository;
import com.example.demo.service.ChunkingService;
import com.example.demo.service.NotionIngestionService;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotionIngestionControllerTest {

    @Mock
    private NotionIngestionService ingestionService;
    @Mock
    private ChunkingService chunkingService;
    @Mock
    private NotionPageContentRepository pageRepo;
    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private NotionIngestionController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    void ingestsRawContentWhenPayloadIsValid() throws Exception {
        OAuth2User principal = principal();
        AppUser user = appUser(21L);
        when(currentUserService.requireUser(principal)).thenReturn(user);

        mockMvc.perform(post("/api/notion/ingestRaw")
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, "n/a", principal.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pageId\":\"page-1\",\"content\":\"hello\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Raw content ingested for pageId=page-1"));

        verify(ingestionService).ingestRawContent(user, "page-1", "hello");
    }

    @Test
    void rejectsRawIngestionWhenPayloadIsIncomplete() throws Exception {
        mockMvc.perform(post("/api/notion/ingestRaw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pageId\":\"page-1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("pageId and content are required"));
    }

    @Test
    void rechunksByPageIdForCurrentUser() throws Exception {
        OAuth2User principal = principal();
        AppUser user = appUser(21L);
        NotionPageContent page = new NotionPageContent("page-1", 21L, "hello");
        setId(page, 77L);
        when(currentUserService.requireUser(principal)).thenReturn(user);
        when(pageRepo.findByPageIdAndAppUserId("page-1", 21L)).thenReturn(Optional.of(page));

        mockMvc.perform(post("/api/notion/rechunkByPageId")
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, "n/a", principal.getAuthorities())))
                        .param("pageId", "page-1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Rechunk triggered for pageId=page-1 (rawNoteId=77)"));

        verify(chunkingService).chunkNote(77L);
    }

    @Test
    void returnsPageContentWhenStored() throws Exception {
        OAuth2User principal = principal();
        AppUser user = appUser(21L);
        NotionPageContent page = new NotionPageContent("page-1", 21L, "hello");
        when(currentUserService.requireUser(principal)).thenReturn(user);
        when(pageRepo.findByPageIdAndAppUserId("page-1", 21L)).thenReturn(Optional.of(page));

        mockMvc.perform(get("/api/notion/page/page-1")
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, "n/a", principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageId").value("page-1"))
                .andExpect(jsonPath("$.content").value("hello"));
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

    private void setId(NotionPageContent page, Long id) {
        try {
            var field = NotionPageContent.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(page, id);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}

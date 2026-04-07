package com.example.demo.controller;

import com.example.demo.entity.AppUser;
import com.example.demo.entity.NotionPageContent;
import com.example.demo.repository.NotionPageContentRepository;
import com.example.demo.service.auth.CurrentUserService;
import com.example.demo.service.ingestion.GoogleDocsIngestionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class GoogleDocsIngestionControllerTest {

    @Mock
    private GoogleDocsIngestionService ingestionService;
    @Mock
    private NotionPageContentRepository pageRepository;
    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private GoogleDocsIngestionController controller;

    private MockMvc mockMvc;
    private OAuth2AuthorizedClient authorizedClient;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver())
                .build();
        ClientRegistration registration = ClientRegistration.withRegistrationId("google")
                .clientId("client")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .scope("openid")
                .build();
        authorizedClient = new OAuth2AuthorizedClient(
                registration,
                "sub-123",
                new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "token", Instant.now(), Instant.now().plusSeconds(3600))
        );
    }

    @Test
    void ingestDocRejectsWhenGoogleClientMissing() {
        OAuth2User principal = principal();
        var response = controller.ingestDoc(principal, Map.of("docId", "doc-1"), null);

        org.assertj.core.api.Assertions.assertThat(response.getStatusCode().value()).isEqualTo(401);
        org.assertj.core.api.Assertions.assertThat(response.getBody()).isEqualTo("Google account is not connected");
    }

    @Test
    void ingestRawStoresContentForCurrentUser() {
        OAuth2User principal = principal();
        AppUser user = appUser(51L);
        when(currentUserService.requireUser(any())).thenReturn(user);

        var response = controller.ingestRaw(principal, Map.of("docId", "doc-1", "content", "hello"));

        org.assertj.core.api.Assertions.assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(ingestionService).ingestRawContent(51L, "doc-1", "hello");
    }

    @Test
    void returnsStoredGoogleDocContent() throws Exception {
        OAuth2User principal = principal();
        AppUser user = appUser(51L);
        NotionPageContent page = new NotionPageContent("gdoc:doc-1", 51L, "hello");
        when(currentUserService.requireUser(any())).thenReturn(user);
        when(pageRepository.findByPageIdAndAppUserId("gdoc:doc-1", 51L)).thenReturn(Optional.of(page));

        mockMvc.perform(get("/api/google-docs/doc/doc-1")
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, "n/a", principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageId").value("gdoc:doc-1"))
                .andExpect(jsonPath("$.content").value("hello"));
    }

    @Test
    void ingestSelectedRejectsEmptyDocIds() {
        OAuth2User principal = principal();
        var response = controller.ingestSelected(principal, authorizedClient, Map.of("docIds", List.of()));

        org.assertj.core.api.Assertions.assertThat(response.getStatusCode().value()).isEqualTo(400);
        org.assertj.core.api.Assertions.assertThat(response.getBody()).isEqualTo("docIds are required");
    }

    @Test
    void ingestSelectedProcessesNonBlankDocIds() {
        OAuth2User principal = principal();
        AppUser user = appUser(51L);
        when(currentUserService.requireUser(any())).thenReturn(user);

        var response = controller.ingestSelected(principal, authorizedClient, Map.of("docIds", List.of("doc-1", " ", "doc-2")));

        org.assertj.core.api.Assertions.assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(ingestionService).ingestDoc(51L, "doc-1", "token");
        verify(ingestionService).ingestDoc(51L, "doc-2", "token");
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

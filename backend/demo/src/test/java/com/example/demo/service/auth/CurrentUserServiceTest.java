package com.example.demo.service.auth;

import com.example.demo.entity.AppUser;
import com.example.demo.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private CurrentUserService service;

    @Test
    void createsNewUserWhenNoMatchingRecordExists() {
        OAuth2User principal = principal(Map.of(
                "sub", "google-123",
                "email", "alice@example.com",
                "name", "Alice",
                "picture", "https://avatar"
        ));
        when(appUserRepository.findByAuthProviderAndProviderUserId("google", "google-123")).thenReturn(Optional.empty());
        when(appUserRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppUser saved = service.requireUser(principal);

        assertThat(saved.getEmail()).isEqualTo("alice@example.com");
        assertThat(saved.getName()).isEqualTo("Alice");
        assertThat(saved.getAuthProvider()).isEqualTo("google");
        assertThat(saved.getProviderUserId()).isEqualTo("google-123");
    }

    @Test
    void reusesExistingUserMatchedByProviderId() {
        OAuth2User principal = principal(Map.of(
                "sub", "google-123",
                "email", "alice@example.com",
                "name", "Alice Updated"
        ));
        AppUser existing = new AppUser("old@example.com", "Old Name", null, "google", "google-123");
        when(appUserRepository.findByAuthProviderAndProviderUserId("google", "google-123")).thenReturn(Optional.of(existing));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppUser saved = service.requireUser(principal);

        assertThat(saved).isSameAs(existing);
        assertThat(saved.getEmail()).isEqualTo("alice@example.com");
        assertThat(saved.getName()).isEqualTo("Alice Updated");
    }

    @Test
    void fallsBackToEmailMatchAndDerivesNameWhenMissing() {
        OAuth2User principal = principal(Map.of(
                "sub", "google-456",
                "email", "fallback@example.com"
        ));
        AppUser existing = new AppUser("fallback@example.com", "Old", null, "google", "legacy-id");
        when(appUserRepository.findByAuthProviderAndProviderUserId("google", "google-456")).thenReturn(Optional.empty());
        when(appUserRepository.findByEmail("fallback@example.com")).thenReturn(Optional.of(existing));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppUser saved = service.requireUser(principal);

        assertThat(saved).isSameAs(existing);
        assertThat(saved.getName()).isEqualTo("fallback");
        assertThat(saved.getProviderUserId()).isEqualTo("google-456");
    }

    @Test
    void rejectsMissingPrincipalOrIncompleteAttributes() {
        assertThatThrownBy(() -> service.requireUser(null)).isInstanceOf(ResponseStatusException.class);

        OAuth2User incomplete = principal(Map.of("email", "missing-sub@example.com"));
        assertThatThrownBy(() -> service.requireUser(incomplete)).isInstanceOf(ResponseStatusException.class);
    }

    private OAuth2User principal(Map<String, Object> attributes) {
        return new DefaultOAuth2User(List.of(() -> "ROLE_USER"), attributes, attributes.containsKey("sub") ? "sub" : "email");
    }
}

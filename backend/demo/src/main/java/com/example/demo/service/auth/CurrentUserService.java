package com.example.demo.service.auth;

import com.example.demo.entity.AppUser;
import com.example.demo.repository.AppUserRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.Instant;

@Service
public class CurrentUserService {

    private final AppUserRepository appUserRepository;

    public CurrentUserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public AppUser requireUser(OAuth2User principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in with Google to access your Second Brain.");
        }

        String providerUserId = stringAttribute(principal, "sub");
        String email = stringAttribute(principal, "email");
        String name = stringAttribute(principal, "name");
        String avatarUrl = stringAttribute(principal, "picture");

        if (providerUserId == null || providerUserId.isBlank() || email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google account details are incomplete. Please sign in again.");
        }

        AppUser user = appUserRepository
                .findByAuthProviderAndProviderUserId("google", providerUserId)
                .or(() -> appUserRepository.findByEmail(email))
                .orElseGet(() -> new AppUser(email, fallbackName(name, email), avatarUrl, "google", providerUserId));

        user.setEmail(email);
        user.setName(fallbackName(name, email));
        user.setAvatarUrl(avatarUrl);
        user.setAuthProvider("google");
        user.setProviderUserId(providerUserId);
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(Instant.now());
        }
        user.setUpdatedAt(Instant.now());

        return appUserRepository.save(user);
    }

    private String stringAttribute(OAuth2User principal, String attributeName) {
        Object value = principal.getAttributes().get(attributeName);
        return value == null ? null : String.valueOf(value).trim();
    }

    private String fallbackName(String name, String email) {
        if (name != null && !name.isBlank()) {
            return name;
        }
        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : email;
    }
}

package com.example.demo.service.auth;

import com.example.demo.dto.GoogleProfileUser;
import com.example.demo.entity.AppUser;
import com.example.demo.repository.UserRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.Instant;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AppUser SaveUserToDB(OAuth2User principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in with Google to access your Second Brain.");
        }

        GoogleProfileUser profile = extractGoogleProfile(principal);
        
        AppUser user = syncUserWithProfile(profile);

        return userRepository.save(user);
    }
 
    private GoogleProfileUser extractGoogleProfile(OAuth2User principal) {
        String userId = getOAuthAttributeAsString(principal, "sub");
        String email = getOAuthAttributeAsString(principal, "email");
        String name = getOAuthAttributeAsString(principal, "name");
        String avatarUrl = getOAuthAttributeAsString(principal, "picture");

        if (userId == null || userId.isBlank() || email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Incomplete Google account details.");
        }

        return new GoogleProfileUser(userId, email, name, avatarUrl);
    }

    private AppUser syncUserWithProfile(GoogleProfileUser profile) {
        AppUser user = userRepository
                .findByAuthProviderAndProviderUserId("google", profile.userId())
                .or(() -> userRepository.findByEmail(profile.email()))
                .orElseGet(AppUser::new);

        user.setEmail(profile.email());
        user.setName(fallbackName(profile.name(), profile.email()));
        user.setAvatarUrl(profile.avatarUrl());
        user.setAuthProvider("google");
        user.setProviderUserId(profile.userId());
        
        Instant now = Instant.now();
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(now);
        }
        user.setUpdatedAt(now);

        return user;
    }

    private String getOAuthAttributeAsString(OAuth2User principal, String attributeName) {
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

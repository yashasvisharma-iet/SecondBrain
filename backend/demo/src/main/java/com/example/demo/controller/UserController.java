package com.example.demo.controller;

import com.example.demo.dto.CurrentUserDto;
import com.example.demo.entity.AppUser;
import com.example.demo.repository.NotionTokenRepository;
import com.example.demo.service.auth.CurrentUserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UserController {

    private final CurrentUserService currentUserService;
    private final NotionTokenRepository notionTokenRepository;

    public UserController(CurrentUserService currentUserService,
                          NotionTokenRepository notionTokenRepository) {
        this.currentUserService = currentUserService;
        this.notionTokenRepository = notionTokenRepository;
    }

    @GetMapping("/me")
    public CurrentUserDto currentUser(@AuthenticationPrincipal OAuth2User principal) {
        AppUser user = currentUserService.requireUser(principal);
        boolean notionConnected = notionTokenRepository.existsByAppUserId(user.getId());
        return new CurrentUserDto(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getAvatarUrl(),
                true,
                notionConnected
        );
    }
}

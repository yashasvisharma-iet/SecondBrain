package com.example.demo.controller;

import com.example.demo.dto.UserDto;
import com.example.demo.entity.AppUser;
import com.example.demo.repository.NotionTokenRepository;
import com.example.demo.service.auth.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;
    private final NotionTokenRepository notionTokenRepository;

    public UserController(UserService userService,
                          NotionTokenRepository notionTokenRepository) {
        this.userService = userService;
        this.notionTokenRepository = notionTokenRepository;
    }

    @GetMapping("/me")
    public UserDto currentUser(@AuthenticationPrincipal OAuth2User principal) {
        AppUser user = userService.SaveUserToDB(principal);
        boolean notionConnected = notionTokenRepository.existsByAppUserId(user.getId());
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getAvatarUrl(),
                true,
                notionConnected
        );
    }
}

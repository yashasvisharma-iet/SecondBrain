package com.example.demo.controller;

import com.example.demo.entity.AppUser;
import com.example.demo.service.NotionOAuthService;
import com.example.demo.service.auth.CurrentUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/oauth/notion")
public class NotionOAuthController {

    private final NotionOAuthService notionOAuthService;
    private final com.example.demo.service.NotionIngestionService ingestionService;
    private final CurrentUserService currentUserService;

    public NotionOAuthController(NotionOAuthService notionOAuthService,
                                 com.example.demo.service.NotionIngestionService ingestionService,
                                 CurrentUserService currentUserService) {
        this.notionOAuthService = notionOAuthService;
        this.ingestionService = ingestionService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/callback")
    public ResponseEntity<Void> callback(@AuthenticationPrincipal OAuth2User principal,
                                         @RequestBody Map<String, String> body) {

        String code = body.get("code");

        if (code == null) {
            return ResponseEntity.badRequest().build();
        }

        AppUser appUser = currentUserService.requireUser(principal);
        String workspaceId = notionOAuthService.exchangeCode(code, appUser);

        String pageId = body.get("pageId");
        if (pageId != null && !pageId.isBlank()) {
            try {
                ingestionService.ingestPage(appUser, workspaceId, pageId);
            } catch (Exception e) {
                System.err.println("Ingestion after OAuth failed: " + e.getMessage());
            }
        } else {
            try {
                ingestionService.ingestRecentPages(appUser, workspaceId, 5);
            } catch (Exception e) {
                System.err.println("Ingestion of recent pages after OAuth failed: " + e.getMessage());
            }
        }

        return ResponseEntity.ok().build();
    }
}

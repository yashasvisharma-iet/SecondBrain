package com.example.demo.controller;

import com.example.demo.entity.AppUser;
import com.example.demo.service.NotionOAuthService;

import com.example.demo.service.auth.UserService;

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
    private final UserService currentUserService;

    public NotionOAuthController(NotionOAuthService notionOAuthService,
                                 com.example.demo.service.NotionIngestionService ingestionService,
                                 UserService currentUserService) {
        this.notionOAuthService = notionOAuthService;
        this.ingestionService = ingestionService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/callback")
    public ResponseEntity<Void> handleOAuthCallback(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestBody Map<String, String> body) {

        String code = body.get("code");
        if (code == null) {
            return ResponseEntity.badRequest().build();
        }

        AppUser appUser = currentUserService.SaveUserToDB(principal);
        String workspaceId = notionOAuthService.exchangeAuthorizationCodeForNotionToken(code, appUser);

        processIngestion(body, appUser, workspaceId);

        return ResponseEntity.ok().build();
    }
    private void processIngestion(Map<String, String> body, AppUser user, String workspaceId) {
        String pageId = body.get("pageId");

        if (hasValidPageId(pageId)) {
            ingestSinglePage(user, workspaceId, pageId);
            return;
        }

        ingestRecentPages(user, workspaceId);
    }

    private boolean hasValidPageId(String pageId) {
        return pageId != null && !pageId.isBlank();
    }

    private void ingestSinglePage(AppUser user, String workspaceId, String pageId) {
        try {
            ingestionService.ingestPage(user, workspaceId, pageId);
        } catch (Exception e) {
            logError("Ingestion after OAuth failed", e);
        }
    }

    private void ingestRecentPages(AppUser user, String workspaceId) {
        try {
            ingestionService.ingestRecentPages(user, workspaceId, 5);
        } catch (Exception e) {
            logError("Ingestion of recent pages after OAuth failed", e);
        }
    }

    private void logError(String message, Exception e) {
        System.err.println(message + ": " + e.getMessage());
    }

}

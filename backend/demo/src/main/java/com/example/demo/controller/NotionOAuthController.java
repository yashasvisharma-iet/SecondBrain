package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.service.NotionOAuthService;

import java.util.Map;

@RestController
@RequestMapping("/api/oauth/notion")
public class NotionOAuthController {

    private final NotionOAuthService notionOAuthService;
    private final com.example.demo.service.NotionIngestionService ingestionService;

    public NotionOAuthController(NotionOAuthService notionOAuthService, com.example.demo.service.NotionIngestionService ingestionService) {
        this.notionOAuthService = notionOAuthService;
        this.ingestionService = ingestionService;
    }

    @PostMapping("/callback")
    public ResponseEntity<Void> callback(@RequestBody Map<String, String> body) {

        String code = body.get("code");

        if (code == null) {
            return ResponseEntity.badRequest().build();
        }

        String workspaceId = notionOAuthService.exchangeCode(code);

        // optional pageId in body: if provided, kick off ingestion immediately
        String pageId = body.get("pageId");
        if (pageId != null && !pageId.isBlank()) {
            try {
                ingestionService.ingestPage(workspaceId, pageId);
            } catch (Exception e) {
                // don't fail the oauth callback if ingestion fails
                System.err.println("Ingestion after OAuth failed: " + e.getMessage());
            }
        }

        return ResponseEntity.ok().build();
    }
}

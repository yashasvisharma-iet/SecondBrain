package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.service.NotionOAuthService;

import java.util.Map;

@RestController
@RequestMapping("/api/oauth/notion")
public class NotionOAuthController {

    private final NotionOAuthService notionOAuthService;

    public NotionOAuthController(NotionOAuthService notionOAuthService) {
        this.notionOAuthService = notionOAuthService;
    }

    @PostMapping("/callback")
    public ResponseEntity<Void> callback(@RequestBody Map<String, String> body) {

        String code = body.get("code");

        if (code == null) {
            return ResponseEntity.badRequest().build();
        }

        notionOAuthService.exchangeCode(code);
        return ResponseEntity.ok().build();
    }
}

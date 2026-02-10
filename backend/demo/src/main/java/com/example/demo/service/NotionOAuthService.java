package com.example.demo.service;

import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.config.NotionConfig;
import com.example.demo.entity.NotionToken;
import com.example.demo.repository.NotionTokenRepository;
import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
public class NotionOAuthService {

    private static final Logger log = LoggerFactory.getLogger(NotionOAuthService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String TOKEN_URL = "https://api.notion.com/v1/oauth/token";

    private final NotionConfig notionConfig;
    private final NotionTokenRepository tokenRepository;

    public NotionOAuthService(NotionConfig notionConfig, NotionTokenRepository tokenRepository) {
        this.notionConfig = notionConfig;
        this.tokenRepository = tokenRepository;
    }

    public String exchangeCode(String code) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(notionConfig.getClientId(), notionConfig.getClientSecret());
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of(
                "grant_type", "authorization_code",
                "code", code,
                "redirect_uri", notionConfig.getRedirectUri()
        );

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response;
        try {
            response = restTemplate.postForEntity(TOKEN_URL, request, String.class);
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    formatJson("Notion token exchange failed", e.getResponseBodyAsString()),
                    e
            );
        }

        String prettyResponse = formatJson("Notion token response", response.getBody());
        log.info(prettyResponse);

        Map<String, Object> res;
        try {
            res = MAPPER.readValue(response.getBody(), new TypeReference<>() {});
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to parse Notion token response",
                    e
            );
        }

    log.info("NOTION ACCESS TOKEN = {}", res.get("access_token"));

    String accessToken = (String) res.get("access_token");
    String workspaceId = (String) res.get("workspace_id");
    String botId = (String) res.get("bot_id");

        // Upsert by workspaceId to avoid duplicate records if callback is received twice.
        // Handle the case where duplicates already exist in DB by keeping the latest and removing others.
        List<NotionToken> existing = tokenRepository.findAllByWorkspaceId(workspaceId);
        if (existing == null || existing.isEmpty()) {
            NotionToken token = new NotionToken(
                    accessToken,
                    workspaceId,
                    botId
            );
            tokenRepository.save(token);
            log.info("Saved new NotionToken for workspaceId={}", workspaceId);
        } else {
            // pick the latest entry by id
            NotionToken latest = existing.stream()
                    .max(Comparator.comparing(NotionToken::getId))
                    .orElse(existing.get(0));
            latest.setAccessToken(accessToken);
            latest.setBotId(botId);
            tokenRepository.save(latest);
            log.info("Updated existing NotionToken (id={}) for workspaceId={}", latest.getId(), workspaceId);

            // if there are older duplicates, delete them
            if (existing.size() > 1) {
                List<NotionToken> toDelete = existing.stream()
                        .filter(t -> !t.getId().equals(latest.getId()))
                        .collect(Collectors.toList());
                tokenRepository.deleteAll(toDelete);
                log.warn("Found and removed {} duplicate NotionToken records for workspaceId={}", toDelete.size(), workspaceId);
            }
        }
        return workspaceId;
    }

    private String formatJson(String prefix, String json) {
        try {
            String pretty = MAPPER
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(MAPPER.readValue(json, Object.class));
            return prefix + ":\n" + pretty;
        } catch (Exception e) {
            return prefix + ": " + json;
        }
    }
}

package com.example.demo.service;

import java.util.Map;

import com.example.demo.entity.AppUser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.config.NotionConfig;
import com.example.demo.entity.NotionToken;
import com.example.demo.repository.NotionTokenRepository;

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

    public String exchangeCode(String code, AppUser appUser) {
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
        } catch (HttpServerErrorException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    formatJson("Notion token exchange failed due to upstream error", e.getResponseBodyAsString()),
                    e
            );
        } catch (RestClientException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Notion token exchange failed due to connectivity issue",
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

        String accessToken = (String) res.get("access_token");
        String workspaceId = (String) res.get("workspace_id");
        String botId = (String) res.get("bot_id");

        if (accessToken == null || accessToken.isBlank() || workspaceId == null || workspaceId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    formatJson("Notion token response missing required fields", response.getBody())
            );
        }

        NotionToken existing = tokenRepository
                .findFirstByWorkspaceIdOrderByIdDesc(workspaceId)
                .orElse(null);
        if (existing == null) {
            NotionToken token = new NotionToken(
                    accessToken,
                    appUser.getId(),
                    workspaceId,
                    botId
            );
            tokenRepository.save(token);
            log.info("Saved new NotionToken for workspaceId={} userId={}", workspaceId, appUser.getId());
        } else {
            existing.setAccessToken(accessToken);
            existing.setAppUserId(appUser.getId());
            existing.setBotId(botId);
            tokenRepository.save(existing);
            log.info("Updated existing NotionToken (id={}) for workspaceId={} userId={}", existing.getId(), workspaceId, appUser.getId());
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

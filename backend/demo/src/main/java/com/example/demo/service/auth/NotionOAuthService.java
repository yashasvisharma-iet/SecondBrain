package com.example.demo.service.auth;

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

    public String exchangeAuthorizationCodeForNotionToken(String code, AppUser user) {
        String responseBody = executeTokenRequest(code);
        logResponse(responseBody);

        Map<String, Object> response = parseResponse(responseBody);
        validateResponse(response);

        return saveOrUpdateToken(response, user);
    }


    private String executeTokenRequest(String code) {
        try {
            return createRestTemplate()
                    .postForEntity(TOKEN_URL, createHttpRequest(code), String.class)
                    .getBody();
        } catch (HttpClientErrorException e) {
            throw buildBadRequest(e);
        } catch (HttpServerErrorException e) {
            throw buildUpstreamError(e);
        } catch (RestClientException e) {
            throw buildConnectivityError(e);
        }
    }

    private void logResponse(String responseBody) {
        log.info(formatJson("Notion token response", responseBody));
    }

    private Map<String, Object> parseResponse(String body) {
        try {
            return MAPPER.readValue(body, new TypeReference<>() {});
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to parse Notion response",
                    e
            );
        }
    }

    private void validateResponse(Map<String, Object> response) {
        if (isInvalid(response.get("access_token")) || isInvalid(response.get("workspace_id"))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    formatJson("Missing required fields", response.toString())
            );
        }
    }

    private String saveOrUpdateToken(Map<String, Object> res, AppUser user) {
        String workspaceId = (String) res.get("workspace_id");
        NotionToken token = findOrCreateToken(workspaceId);

        updateToken(token, res, user);
        tokenRepository.save(token);

        logTokenAction(token, workspaceId, user);
        return workspaceId;
    }

    // ================= LOW LEVEL =================

    private RestTemplate createRestTemplate() {
        return new RestTemplate();
    }

    private HttpEntity<Map<String, String>> createHttpRequest(String code) {
        return new HttpEntity<>(createRequestBody(code), createHeaders());
    }

    private Map<String, String> createRequestBody(String code) {
        return Map.of(
                "grant_type", "authorization_code",
                "code", code,
                "redirect_uri", notionConfig.getRedirectUri()
        );
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(notionConfig.getClientId(), notionConfig.getClientSecret());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private NotionToken findOrCreateToken(String workspaceId) {
        return tokenRepository
                .findFirstByWorkspaceIdOrderByIdDesc(workspaceId)
                .orElse(new NotionToken());
    }

    private void updateToken(NotionToken token, Map<String, Object> res, AppUser user) {
        token.setAccessToken((String) res.get("access_token"));
        token.setWorkspaceId((String) res.get("workspace_id"));
        token.setBotId((String) res.get("bot_id"));
        token.setAppUserId(user.getId());
    }

    private void logTokenAction(NotionToken token, String workspaceId, AppUser user) {
        if (token.getId() == null) {
            log.info("Saved new token for workspaceId={} userId={}", workspaceId, user.getId());
        } else {
            log.info("Updated token id={} workspaceId={} userId={}",
                    token.getId(), workspaceId, user.getId());
        }
    }

    private boolean isInvalid(Object value) {
        return value == null || value.toString().isBlank();
    }

    //exceptions

    private ResponseStatusException buildBadRequest(HttpClientErrorException e) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                formatJson("Token exchange failed", e.getResponseBodyAsString()),
                e
        );
    }

    private ResponseStatusException buildUpstreamError(HttpServerErrorException e) {
        return new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                formatJson("Upstream error", e.getResponseBodyAsString()),
                e
        );
    }

    private ResponseStatusException buildConnectivityError(RestClientException e) {
        return new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Connectivity issue during token exchange",
                e
        );
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

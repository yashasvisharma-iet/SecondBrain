package com.example.demo.service;

import com.example.demo.entity.AppUser;
import com.example.demo.entity.NotionPageContent;
import com.example.demo.entity.NotionToken;
import com.example.demo.repository.NotionPageContentRepository;
import com.example.demo.repository.NotionTokenRepository;
import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

@Service
public class NotionIngestionService {

    private static final String NOTION_BLOCKS_URL = "https://api.notion.com/v1/blocks/%s/children";
    private static final String NOTION_SEARCH_URL = "https://api.notion.com/v1/search";

    private final NotionTokenRepository tokenRepository;
    private final NotionPageContentRepository contentRepository;
    private final ChunkingService chunkingService;
    private final RestTemplate restTemplate = new RestTemplate();

    public NotionIngestionService(
            NotionTokenRepository tokenRepository,
            NotionPageContentRepository contentRepository,
            ChunkingService chunkingService
    ) {
        this.tokenRepository = tokenRepository;
        this.contentRepository = contentRepository;
        this.chunkingService = chunkingService;
    }

    public void ingestPage(AppUser appUser, String workspaceId, String pageId) {
        NotionToken token = tokenRepository
                .findFirstByWorkspaceIdAndAppUserIdOrderByIdDesc(workspaceId, appUser.getId())
                .orElseThrow(() -> new RuntimeException("No Notion token found for workspace " + workspaceId));

        JsonNode blocks = fetchBlocks(pageId, token.getAccessToken());
        String text = extractPlainText(blocks);
        upsertPage(appUser.getId(), pageId, text);
    }

    public void ingestRawContent(AppUser appUser, String pageId, String rawContent) {
        upsertPage(appUser.getId(), pageId, rawContent);
    }

    public void ingestRecentPages(AppUser appUser, String workspaceId, int limit) {
        NotionToken token = tokenRepository
                .findFirstByWorkspaceIdAndAppUserIdOrderByIdDesc(workspaceId, appUser.getId())
                .orElseThrow(() -> new RuntimeException("No Notion token found for workspace " + workspaceId));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token.getAccessToken());
        headers.set("Notion-Version", "2022-06-28");
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"query\":\"\",\"sort\":{\"direction\":\"descending\",\"timestamp\":\"last_edited_time\"},\"filter\":{\"value\":\"page\",\"property\":\"object\"}}";

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<JsonNode> response = restTemplate.exchange(NOTION_SEARCH_URL, HttpMethod.POST, entity, JsonNode.class);
        JsonNode resp = response.getBody();
        if (resp == null || !resp.has("results")) {
            return;
        }

        int count = 0;
        for (JsonNode item : resp.get("results")) {
            if (count >= limit) {
                break;
            }
            if (!item.has("object") || !"page".equals(item.get("object").asText())) {
                continue;
            }
            String pageId = item.get("id").asText();
            try {
                ingestPage(appUser, workspaceId, pageId);
            } catch (Exception e) {
                System.err.println("NotionIngestionService.ingestRecentPages: failed to ingest pageId=" + pageId + " -> " + e.getMessage());
            }
            count++;
        }
    }

    private void upsertPage(Long appUserId, String pageId, String content) {
        NotionPageContent pageContent = contentRepository
                .findByPageId(pageId)
                .or(() -> contentRepository.findByPageIdAndAppUserId(pageId, appUserId))
                .orElse(new NotionPageContent(pageId, appUserId, content));

        pageContent.setAppUserId(appUserId);
        pageContent.setContent(content);
        pageContent.setSyncedAt(Instant.now());

        NotionPageContent saved;
        try {
            saved = contentRepository.save(pageContent);
        } catch (DataIntegrityViolationException dive) {
            NotionPageContent existing = contentRepository.findByPageId(pageId)
                    .or(() -> contentRepository.findByPageIdAndAppUserId(pageId, appUserId))
                    .orElseThrow(() -> new RuntimeException("Failed to upsert page content after unique constraint", dive));
            existing.setAppUserId(appUserId);
            existing.setContent(content);
            existing.setSyncedAt(Instant.now());
            saved = contentRepository.save(existing);
        }

        try {
            chunkingService.chunkNote(saved.getId());
        } catch (Exception e) {
            System.err.println("Chunking failed for pageId=" + pageId + ": " + e.getMessage());
        }
    }

    private JsonNode fetchBlocks(String pageId, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("Notion-Version", "2022-06-28");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                String.format(NOTION_BLOCKS_URL, pageId),
                HttpMethod.GET,
                entity,
                JsonNode.class
        );

        return response.getBody();
    }

    private String extractPlainText(JsonNode blocksResponse) {
        if (blocksResponse == null || !blocksResponse.has("results")) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (JsonNode block : blocksResponse.get("results")) {
            String type = block.path("type").asText();
            JsonNode richText = block.path(type).path("rich_text");

            if (!richText.isArray()) {
                continue;
            }

            for (JsonNode rt : richText) {
                String text = rt.path("plain_text").asText();
                if (!text.isBlank()) {
                    sb.append(text).append("\n");
                }
            }
        }

        return sb.toString().trim();
    }
}

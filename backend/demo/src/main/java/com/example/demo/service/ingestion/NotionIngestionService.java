package com.example.demo.service.ingestion;

import com.example.demo.entity.AppUser;
import com.example.demo.entity.NotionPageContent;
import com.example.demo.entity.NotionToken;
import com.example.demo.repository.NotionPageContentRepository;
import com.example.demo.repository.NotionTokenRepository;
import com.example.demo.service.chunkingAndEmbedding.ChunkingService;
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

    public void ingestMultiplePages(AppUser appUser, String workspaceId, int limit) {
        NotionToken token = findToken(appUser.getId(), workspaceId);
        JsonNode resp = searchPages(token);

        processPages(appUser, workspaceId, resp, limit);
    }

    public void ingestPage(AppUser user, String workspaceId, String pageId) {
        NotionToken token = findToken(user.getId(), workspaceId);
        String content = fetchPageContent(pageId, token);

        savePage(user.getId(), pageId, content);
    }

    public void ingestRawContent(AppUser user, String pageId, String rawContent) {
        savePage(user.getId(), pageId, rawContent);
    }

    // -------------------- CORE --------------------
        private void processPages(AppUser user, String workspaceId, JsonNode results, int limit) {
        if (isInvalidResults(results)) return;

        int count = 0;
        for (JsonNode page : results.get("results")) {
            if (isLimitReached(count, limit)) break;
            if (isNotPage(page)) continue;

            processSinglePage(user, workspaceId, page);
            count++;
        }
    }

    private void processSinglePage(AppUser user, String workspaceId, JsonNode page) {
        String pageId = page.get("id").asText();

        try {
            ingestPage(user, workspaceId, pageId);
        } catch (Exception e) {
            logIngestionFailure(pageId, e);
        }
    }

    private JsonNode searchPages(NotionToken token) {
        HttpHeaders headers = buildHeaders(token.getAccessToken());
        String body = buildSearchBody();

         HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                NOTION_SEARCH_URL,
                HttpMethod.POST,
                request,
                JsonNode.class
        );

        return response.getBody();
    }

    private HttpHeaders buildHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("Notion-Version", "2022-06-28");
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String buildSearchBody() {
        return "{\"query\":\"\",\"sort\":{\"direction\":\"descending\",\"timestamp\":\"last_edited_time\"},\"filter\":{\"value\":\"page\",\"property\":\"object\"}}";
    }


    private NotionToken findToken(Long appUserId, String workspaceId) {
        return tokenRepository
                .findFirstByWorkspaceIdAndAppUserIdOrderByIdDesc(workspaceId, appUserId)
                .orElseThrow(() -> new RuntimeException("No Notion token found for workspace " + workspaceId));
    }


    // -------------------- PAGE CONTENT --------------------

    private String fetchPageContent(String pageId, NotionToken token) {
        JsonNode blocks = fetchBlocks(pageId, token.getAccessToken());
        return extractPlainText(blocks);
    }

    private JsonNode fetchBlocks(String pageId, String token) {
        HttpEntity<Void> request = new HttpEntity<>(buildHeaders(token));

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                String.format(NOTION_BLOCKS_URL, pageId),
                HttpMethod.GET,
                request,
                JsonNode.class
        );

        return response.getBody();
    }
    private String extractPlainText(JsonNode blocksResponse) {
        if (isInvalidBlocks(blocksResponse)) return "";

        StringBuilder content = new StringBuilder();

        for (JsonNode block : blocksResponse.get("results")) {
            String type = block.path("type").asText();
            JsonNode richText = block.path(type).path("rich_text");

            if (!richText.isArray()) continue;

            for (JsonNode textNode : richText) {
                String text = textNode.path("plain_text").asText();
                if (text != null && !text.isBlank()) {
                    content.append(text).append("\n");
                }
            }
        }

        return content.toString().trim();
    }

    // -------------------- PERSIST --------------------

    private void savePage(Long userId, String pageId, String content) {
        NotionPageContent page = findOrCreatePage(userId, pageId);

        updatePage(page, userId, content);
        NotionPageContent saved = persistPage(page);

        triggerChunking(saved.getId(), pageId);
    }

    private NotionPageContent findOrCreatePage(Long userId, String pageId) {
        return contentRepository
                .findByPageIdAndAppUserId(pageId, userId)
                .or(() -> contentRepository.findByPageId(pageId))
                .orElse(new NotionPageContent(pageId, userId, ""));
    }

    private void updatePage(NotionPageContent page, Long userId, String content) {
        page.setAppUserId(userId);
        page.setContent(content);
        page.setSyncedAt(Instant.now());
    }

    private NotionPageContent persistPage(NotionPageContent page) {
        try {
            return contentRepository.save(page);
        } catch (DataIntegrityViolationException e) {
            return recoverAndSave(page);
        }
    }

    private NotionPageContent recoverAndSave(NotionPageContent page) {
        NotionPageContent existing = contentRepository
                .findByPageIdAndAppUserId(page.getPageId(), page.getAppUserId())
                .or(() -> contentRepository.findByPageId(page.getPageId()))
                .orElseThrow(() -> new RuntimeException("Upsert failed"));

        updatePage(existing, page.getAppUserId(), page.getContent());
        return contentRepository.save(existing);
    }

    // -------------------- TRIGGER CHUNKING --------------------
    private void triggerChunking(Long id, String pageId) {
        try {
            chunkingService.chunkNote(id);
        } catch (Exception e) {
            logChunkingFailure(pageId, e);
        }
    }

    // -------------------- VALIDATION & LOG --------------------

    private boolean isInvalidResults(JsonNode results) {
        return results == null || !results.has("results");
    }

    private boolean isLimitReached(int count, int limit) {
        return count >= limit;
    }

    private boolean isNotPage(JsonNode node) {
        return !node.has("object") || !"page".equals(node.get("object").asText());
    }

    private void logIngestionFailure(String pageId, Exception e) {
        System.err.println("Failed to ingest page: " + pageId + " -> " + e.getMessage());
    }

    private void logChunkingFailure(String pageId, Exception e) {
        System.err.println("Chunking failed: " + pageId + " -> " + e.getMessage());
    }
    private boolean isInvalidBlocks(JsonNode blocks) {
        return blocks == null || !blocks.has("results");
    }
}

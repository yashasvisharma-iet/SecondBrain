package com.example.demo.service;

import com.example.demo.entity.NotionPageContent;
import com.example.demo.entity.NotionToken;
import com.example.demo.repository.NotionPageContentRepository;
import com.example.demo.repository.NotionTokenRepository;
import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

@Service
public class NotionIngestionService {

    private static final String NOTION_BLOCKS_URL =
            "https://api.notion.com/v1/blocks/%s/children";

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

    public void ingestPage(String workspaceId, String pageId) {

        // 1️⃣ Resolve token
        NotionToken token = tokenRepository
                .findAllByWorkspaceId(workspaceId)
                .stream()
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException("No Notion token found for workspace " + workspaceId)
                );

        // 2️⃣ Fetch blocks from Notion
        JsonNode blocks = fetchBlocks(pageId, token.getAccessToken());

        // 3️⃣ Extract plain text
        String text = extractPlainText(blocks);

        // 4️⃣ Upsert into DB
        NotionPageContent pageContent = contentRepository
                .findByPageId(pageId)
                .orElse(new NotionPageContent(pageId, text));

        pageContent.setContent(text);
        pageContent.setSyncedAt(Instant.now());

        NotionPageContent saved = contentRepository.save(pageContent);

        // trigger chunking synchronously for now
        try {
            chunkingService.chunkNote(saved.getId());
        } catch (Exception e) {
            // log and continue; chunking should not break ingestion
            // use System.err to avoid bringing in logger here
            System.err.println("Chunking failed for pageId=" + pageId + ": " + e.getMessage());
        }
    }

    // ---------------- helpers ----------------

    private JsonNode fetchBlocks(String pageId, String accessToken) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("Notion-Version", "2022-06-28");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<JsonNode> response =
                restTemplate.exchange(
                        String.format(NOTION_BLOCKS_URL, pageId),
                        HttpMethod.GET,
                        entity,
                        JsonNode.class
                );

        return response.getBody();
    }

    private String extractPlainText(JsonNode blocksResponse) {

        StringBuilder sb = new StringBuilder();

        for (JsonNode block : blocksResponse.get("results")) {
            String type = block.get("type").asText();
            JsonNode richText = block.get(type).get("rich_text");

            if (richText == null || !richText.isArray()) continue;

            for (JsonNode rt : richText) {
                String text = rt.get("plain_text").asText();
                if (!text.isBlank()) {
                    sb.append(text).append("\n");
                }
            }
        }

        return sb.toString().trim();
    }
}

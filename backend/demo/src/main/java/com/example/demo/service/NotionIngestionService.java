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
import org.springframework.dao.DataIntegrityViolationException;

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

    public void ingestPage(String workspaceId, String pageId) {
        System.out.println("NotionIngestionService.ingestPage: workspaceId=" + workspaceId + " pageId=" + pageId);

        NotionToken token = tokenRepository
                .findAllByWorkspaceId(workspaceId)
                .stream()
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException("No Notion token found for workspace " + workspaceId)
                );

        JsonNode blocks = fetchBlocks(pageId, token.getAccessToken());
        if (blocks == null) {
            System.err.println("NotionIngestionService: fetchBlocks returned null for pageId=" + pageId);
        } else {
            System.out.println("NotionIngestionService: fetched blocks for pageId=" + pageId + " (results size=" + (blocks.has("results") ? blocks.get("results").size() : 0) + ")");
        }

        String text = extractPlainText(blocks);

    NotionPageContent pageContent = contentRepository
        .findByPageId(pageId)
        .orElse(new NotionPageContent(pageId, text));

    pageContent.setContent(text);
    pageContent.setSyncedAt(Instant.now());

    //saving in DB and chunking 

    NotionPageContent saved;
    try {
        saved = contentRepository.save(pageContent);
    } catch (DataIntegrityViolationException dive) {
        NotionPageContent existing = contentRepository.findByPageId(pageId)
            .orElseThrow(() -> new RuntimeException("Failed to upsert page content after unique constraint", dive));
        existing.setContent(text);
        existing.setSyncedAt(Instant.now());
        saved = contentRepository.save(existing);
    }

        // trigger chunking synchronously for now
        try {
            chunkingService.chunkNote(saved.getId());
        } catch (Exception e) {
            System.err.println("Chunking failed for pageId=" + pageId + ": " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    //Ingest Raw Content Sent by Client
    public void ingestRawContent(String pageId, String rawContent) {
        System.out.println("NotionIngestionService.ingestRawContent: pageId=" + pageId + " contentLength=" + (rawContent == null ? 0 : rawContent.length()));

    NotionPageContent pageContent = contentRepository
        .findByPageId(pageId)
        .orElse(new NotionPageContent(pageId, rawContent));

    pageContent.setContent(rawContent);
    pageContent.setSyncedAt(java.time.Instant.now());

    NotionPageContent saved;
        try {
            saved = contentRepository.save(pageContent);
        } catch (DataIntegrityViolationException dive) {
           
            System.err.println("NotionIngestionService.ingestRawContent: unique constraint on pageId=" + pageId + ", attempting update path");
            NotionPageContent existing = contentRepository.findByPageId(pageId)
                    .orElseThrow(() -> new RuntimeException("Failed to upsert raw page content after unique constraint", dive));
            existing.setContent(rawContent);
            existing.setSyncedAt(java.time.Instant.now());
            saved = contentRepository.save(existing);
        }

        System.out.println("NotionIngestionService.ingestRawContent: saved pageId=" + pageId + " id=" + saved.getId());

        try {
            chunkingService.chunkNote(saved.getId());
        } catch (Exception e) {
            System.err.println("Chunking failed for raw pageId=" + pageId + ": " + e.getMessage());
        }
    }



    // ---------------- helpers ----------------
    //fetchBlocks - given a pageId, call Notion API to fetch the blocks (content) of the page. 
    //ingestRecentPages - (Just after OAuth) sfetch recent pages from the workspace using Notion Search API and ingest them (used after OAuth to quickly index some content for the user)

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


    public void ingestRecentPages(String workspaceId, int limit) {
        System.out.println("NotionIngestionService.ingestRecentPages: workspaceId=" + workspaceId + " limit=" + limit);

        NotionToken token = tokenRepository
                .findAllByWorkspaceId(workspaceId)
                .stream()
                .findFirst()
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
            System.out.println("NotionIngestionService.ingestRecentPages: no results from search for workspace=" + workspaceId);
            return;
        }

        int count = 0;
        for (JsonNode item : resp.get("results")) {
            if (count >= limit) break;
            if (!item.has("object")) continue;
            String obj = item.get("object").asText();
            if (!"page".equals(obj)) continue;
            String pageId = item.get("id").asText();
            try {
                System.out.println("NotionIngestionService.ingestRecentPages: ingesting pageId=" + pageId);
                ingestPage(workspaceId, pageId);
            } catch (Exception e) {
                System.err.println("NotionIngestionService.ingestRecentPages: failed to ingest pageId=" + pageId + " -> " + e.getMessage());
                e.printStackTrace(System.err);
            }
            count++;
        }
    }
}

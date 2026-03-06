package com.example.demo.service;

import com.example.demo.entity.NotionPageContent;
import com.example.demo.repository.NotionPageContentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Iterator;

@Service
public class GoogleDocsIngestionService {

    private static final String GOOGLE_DOCS_URL = "https://docs.googleapis.com/v1/documents/%s";
    private static final String GOOGLE_DOC_PREFIX = "gdoc:";

    private final NotionPageContentRepository contentRepository;
    private final ChunkingService chunkingService;
    private final RestTemplate restTemplate = new RestTemplate();

    public GoogleDocsIngestionService(NotionPageContentRepository contentRepository,
                                      ChunkingService chunkingService) {
        this.contentRepository = contentRepository;
        this.chunkingService = chunkingService;
    }

    public void ingestDoc(String docId, String accessToken) {
        String pageId = toGoogleDocPageId(docId);
        JsonNode document = fetchDocument(docId, accessToken);
        String text = extractPlainText(document);
        upsertContentAndChunk(pageId, text);
    }

    public void ingestRawContent(String docId, String rawContent) {
        String pageId = toGoogleDocPageId(docId);
        upsertContentAndChunk(pageId, rawContent);
    }

    private JsonNode fetchDocument(String docId, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                String.format(GOOGLE_DOCS_URL, docId),
                HttpMethod.GET,
                entity,
                JsonNode.class
        );

        return response.getBody();
    }

    private String extractPlainText(JsonNode documentResponse) {
        if (documentResponse == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        appendTextRuns(documentResponse.path("body").path("content"), sb);
        return sb.toString().trim();
    }

    private void appendTextRuns(JsonNode node, StringBuilder sb) {
        if (node == null || node.isNull()) {
            return;
        }

        if (node.isObject()) {
            JsonNode textRun = node.get("textRun");
            if (textRun != null && textRun.has("content")) {
                String text = textRun.get("content").asText();
                if (!text.isBlank()) {
                    sb.append(text);
                }
            }

            Iterator<JsonNode> children = node.elements();
            while (children.hasNext()) {
                appendTextRuns(children.next(), sb);
            }
            return;
        }

        if (node.isArray()) {
            for (JsonNode child : node) {
                appendTextRuns(child, sb);
            }
        }
    }

    private void upsertContentAndChunk(String pageId, String content) {
        NotionPageContent pageContent = contentRepository
                .findByPageId(pageId)
                .orElse(new NotionPageContent(pageId, content));

        pageContent.setContent(content);
        pageContent.setSyncedAt(Instant.now());

        NotionPageContent saved;
        try {
            saved = contentRepository.save(pageContent);
        } catch (DataIntegrityViolationException dive) {
            NotionPageContent existing = contentRepository.findByPageId(pageId)
                    .orElseThrow(() -> new RuntimeException("Failed to upsert Google Doc content after unique constraint", dive));
            existing.setContent(content);
            existing.setSyncedAt(Instant.now());
            saved = contentRepository.save(existing);
        }

        try {
            chunkingService.chunkNote(saved.getId());
        } catch (Exception e) {
            System.err.println("Chunking failed for Google Doc pageId=" + pageId + ": " + e.getMessage());
        }
    }

    private String toGoogleDocPageId(String docId) {
        return GOOGLE_DOC_PREFIX + docId;
    }
}

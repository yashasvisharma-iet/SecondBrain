package com.example.demo.controller;

import com.example.demo.dto.NoteContentDto;
import com.example.demo.repository.NotionPageContentRepository;
import com.example.demo.service.GoogleDocsIngestionService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/google-docs")
public class GoogleDocsIngestionController {

    private static final String GOOGLE_DOC_PREFIX = "gdoc:";
    private static final String GOOGLE_DRIVE_DOCS_LIST_URL = "https://www.googleapis.com/drive/v3/files?q=mimeType='application/vnd.google-apps.document' and trashed=false&fields=files(id,name,modifiedTime)&orderBy=modifiedTime desc&pageSize=25";

    private final GoogleDocsIngestionService ingestionService;
    private final NotionPageContentRepository pageRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public GoogleDocsIngestionController(GoogleDocsIngestionService ingestionService,
                                         NotionPageContentRepository pageRepository) {
        this.ingestionService = ingestionService;
        this.pageRepository = pageRepository;
    }

    @PostMapping("/ingest")
    public ResponseEntity<String> ingestDoc(@RequestParam String docId,
                                            @RequestParam String accessToken) {
        if (docId.isBlank() || accessToken.isBlank()) {
            return ResponseEntity.badRequest().body("docId and accessToken are required");
        }

        ingestionService.ingestDoc(docId, accessToken);
        return ResponseEntity.ok("Google Doc ingested successfully for docId=" + docId);
    }

    @PostMapping("/ingestRaw")
    public ResponseEntity<String> ingestRaw(@RequestBody Map<String, String> body) {
        String docId = body.get("docId");
        String content = body.get("content");

        if (docId == null || docId.isBlank() || content == null) {
            return ResponseEntity.badRequest().body("docId and content are required");
        }

        ingestionService.ingestRawContent(docId, content);
        return ResponseEntity.ok("Raw Google Doc content ingested for docId=" + docId);
    }

    @GetMapping("/doc/{docId}")
    public ResponseEntity<NoteContentDto> getDocByDocId(@PathVariable String docId) {
        String pageId = GOOGLE_DOC_PREFIX + docId;
        return pageRepository.findByPageId(pageId)
                .map(page -> ResponseEntity.ok(new NoteContentDto(page.getPageId(), page.getContent())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/list")
    public ResponseEntity<?> listDocs(@RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient,
                                      @AuthenticationPrincipal OAuth2User principal) {
        if (principal == null || authorizedClient == null || authorizedClient.getAccessToken() == null) {
            return ResponseEntity.status(401).body("Google account is not connected");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authorizedClient.getAccessToken().getTokenValue());

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                GOOGLE_DRIVE_DOCS_LIST_URL,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                JsonNode.class
        );

        JsonNode files = response.getBody() == null ? null : response.getBody().path("files");
        List<Map<String, String>> docs = new ArrayList<>();
        if (files != null && files.isArray()) {
            for (JsonNode file : files) {
                Map<String, String> doc = new HashMap<>();
                doc.put("id", file.path("id").asText());
                doc.put("name", file.path("name").asText("Untitled"));
                doc.put("modifiedTime", file.path("modifiedTime").asText(""));
                docs.add(doc);
            }
        }

        return ResponseEntity.ok(docs);
    }

    @PostMapping("/ingest-selected")
    public ResponseEntity<?> ingestSelected(@RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient,
                                            @RequestBody Map<String, List<String>> body) {
        if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
            return ResponseEntity.status(401).body("Google account is not connected");
        }

        List<String> docIds = body.getOrDefault("docIds", List.of());
        if (docIds.isEmpty()) {
            return ResponseEntity.badRequest().body("docIds are required");
        }

        String accessToken = authorizedClient.getAccessToken().getTokenValue();
        for (String docId : docIds) {
            if (docId != null && !docId.isBlank()) {
                ingestionService.ingestDoc(docId, accessToken);
            }
        }

        return ResponseEntity.ok("Selected Google Docs ingested successfully");
    }
}

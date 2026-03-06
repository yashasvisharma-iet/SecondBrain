package com.example.demo.controller;

import com.example.demo.dto.NoteContentDto;
import com.example.demo.repository.NotionPageContentRepository;
import com.example.demo.service.GoogleDocsIngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/google-docs")
public class GoogleDocsIngestionController {

    private static final String GOOGLE_DOC_PREFIX = "gdoc:";

    private final GoogleDocsIngestionService ingestionService;
    private final NotionPageContentRepository pageRepository;

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
}

package com.example.demo.controller;

import com.example.demo.service.NotionIngestionService;
import com.example.demo.service.ChunkingService;
import com.example.demo.repository.NotionPageContentRepository;
import com.example.demo.entity.NotionPageContent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notion")
public class NotionIngestionController {

    private final NotionIngestionService ingestionService;
    private final ChunkingService chunkingService;
    private final NotionPageContentRepository pageRepo;

    public NotionIngestionController(NotionIngestionService ingestionService,
                                     ChunkingService chunkingService,
                                     NotionPageContentRepository pageRepo) {
        this.ingestionService = ingestionService;
        this.chunkingService = chunkingService;
        this.pageRepo = pageRepo;
    }

    @PostMapping("/ingest")
    public ResponseEntity<String> ingestPage(
            @RequestParam String workspaceId,
            @RequestParam String pageId
    ) {
        ingestionService.ingestPage(workspaceId, pageId);
        return ResponseEntity.ok("Page ingested successfully");
    }

    @PostMapping("/rechunk")
    public ResponseEntity<String> rechunkByRawNoteId(@RequestParam Long rawNoteId) {
        chunkingService.chunkNote(rawNoteId);
        return ResponseEntity.ok("Rechunk triggered for rawNoteId=" + rawNoteId);
    }

    @PostMapping("/rechunkByPageId")
    public ResponseEntity<String> rechunkByPageId(@RequestParam String pageId) {
        NotionPageContent page = pageRepo.findByPageId(pageId)
                .orElseThrow(() -> new IllegalArgumentException("pageId not found: " + pageId));
        chunkingService.chunkNote(page.getId());
        return ResponseEntity.ok("Rechunk triggered for pageId=" + pageId + " (rawNoteId=" + page.getId() + ")");
    }
}

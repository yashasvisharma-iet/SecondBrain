package com.example.demo.controller;

import com.example.demo.service.NotionIngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notion")
public class NotionIngestionController {

    private final NotionIngestionService ingestionService;

    public NotionIngestionController(NotionIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<String> ingestPage(
            @RequestParam String workspaceId,
            @RequestParam String pageId
    ) {
        ingestionService.ingestPage(workspaceId, pageId);
        return ResponseEntity.ok("Page ingested successfully");
    }
}

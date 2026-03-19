package com.example.demo.controller;

import com.example.demo.dto.NoteContentDto;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.NotionPageContent;
import com.example.demo.repository.NotionPageContentRepository;
import com.example.demo.service.ChunkingService;
import com.example.demo.service.NotionIngestionService;
import com.example.demo.service.auth.CurrentUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/notion")
public class NotionIngestionController {

    private final NotionIngestionService ingestionService;
    private final ChunkingService chunkingService;
    private final NotionPageContentRepository pageRepo;
    private final CurrentUserService currentUserService;

    public NotionIngestionController(NotionIngestionService ingestionService,
                                     ChunkingService chunkingService,
                                     NotionPageContentRepository pageRepo,
                                     CurrentUserService currentUserService) {
        this.ingestionService = ingestionService;
        this.chunkingService = chunkingService;
        this.pageRepo = pageRepo;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<String> ingestPage(@AuthenticationPrincipal OAuth2User principal,
                                             @RequestParam String workspaceId,
                                             @RequestParam String pageId) {
        AppUser user = currentUserService.requireUser(principal);
        ingestionService.ingestPage(user, workspaceId, pageId);
        return ResponseEntity.ok("Page ingested successfully");
    }

    @PostMapping("/ingestRaw")
    public ResponseEntity<String> ingestRawFromFrontend(@AuthenticationPrincipal OAuth2User principal,
                                                        @RequestBody Map<String, String> body) {
        String pageId = body.get("pageId");
        String content = body.get("content");

        if (pageId == null || pageId.isBlank() || content == null) {
            return ResponseEntity.badRequest().body("pageId and content are required");
        }

        AppUser user = currentUserService.requireUser(principal);
        ingestionService.ingestRawContent(user, pageId, content);

        return ResponseEntity.ok("Raw content ingested for pageId=" + pageId);
    }

    @PostMapping("/rechunk")
    public ResponseEntity<String> rechunkByRawNoteId(@RequestParam Long rawNoteId) {
        chunkingService.chunkNote(rawNoteId);
        return ResponseEntity.ok("Rechunk triggered for rawNoteId=" + rawNoteId);
    }

    @PostMapping("/rechunkByPageId")
    public ResponseEntity<String> rechunkByPageId(@AuthenticationPrincipal OAuth2User principal,
                                                  @RequestParam String pageId) {
        AppUser user = currentUserService.requireUser(principal);
        NotionPageContent page = pageRepo.findByPageIdAndAppUserId(pageId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("pageId not found: " + pageId));
        chunkingService.chunkNote(page.getId());
        return ResponseEntity.ok("Rechunk triggered for pageId=" + pageId + " (rawNoteId=" + page.getId() + ")");
    }

    @GetMapping("/page/{pageId}")
    public ResponseEntity<NoteContentDto> getPageByPageId(@AuthenticationPrincipal OAuth2User principal,
                                                          @PathVariable String pageId) {
        AppUser user = currentUserService.requireUser(principal);
        return pageRepo
                .findByPageIdAndAppUserId(pageId, user.getId())
                .map(page -> ResponseEntity.ok(new NoteContentDto(page.getPageId(), page.getContent())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

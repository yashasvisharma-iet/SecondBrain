package com.example.demo.service;

import com.example.demo.entity.NotionPageContent;
import com.example.demo.entity.TextChunk;
import com.example.demo.repository.NotionPageContentRepository;
import com.example.demo.repository.TextChunkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChunkingService {

    private final NotionPageContentRepository pageRepo;
    private final TextChunkRepository chunkRepo;
    private final TextChunker chunker;

    public ChunkingService(
            NotionPageContentRepository pageRepo,
            TextChunkRepository chunkRepo,
            TextChunker chunker
    ) {
        this.pageRepo = pageRepo;
        this.chunkRepo = chunkRepo;
        this.chunker = chunker;
    }

    @Transactional
    public void chunkNote(Long rawNoteId) {
        if (rawNoteId == null) return;

        if (chunkRepo.existsByRawNoteId(rawNoteId)) {
            return; // idempotent
        }

        NotionPageContent page = pageRepo.findById(rawNoteId)
                .orElseThrow(() -> new IllegalArgumentException("NotionPageContent not found: " + rawNoteId));

        List<String> chunks = chunker.chunk(page.getContent());

        int index = 0;
        for (String content : chunks) {
            TextChunk chunk = new TextChunk();
            chunk.setRawNoteId(rawNoteId);
            chunk.setChunkIndex(index++);
            chunk.setContent(content);
            chunkRepo.save(chunk);
        }
    }
}

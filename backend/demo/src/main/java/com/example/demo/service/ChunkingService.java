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
    private final AimlEmbeddingClient aimlEmbeddingClient;
    private final PostgresVectorStoreService vectorStoreService;

    public ChunkingService(
            NotionPageContentRepository pageRepo,
            TextChunkRepository chunkRepo,
            TextChunker chunker,
            AimlEmbeddingClient aimlEmbeddingClient,
            PostgresVectorStoreService vectorStoreService
    ) {
        this.pageRepo = pageRepo;
        this.chunkRepo = chunkRepo;
        this.chunker = chunker;
        this.aimlEmbeddingClient = aimlEmbeddingClient;
        this.vectorStoreService = vectorStoreService;
    }

    @Transactional
    public void chunkNote(Long rawNoteId) {
        if (rawNoteId == null) return;
        // Always remove existing chunks for this raw note and re-chunk based on current content.
        // This ensures updated or oversized documents are re-processed instead of being skipped
        // because chunks already exist.
        if (chunkRepo.existsByRawNoteId(rawNoteId)) {
            chunkRepo.deleteByRawNoteId(rawNoteId);
        }
        vectorStoreService.deleteByRawNoteId(rawNoteId);

        NotionPageContent page = pageRepo.findById(rawNoteId)
                .orElseThrow(() -> new IllegalArgumentException("NotionPageContent not found: " + rawNoteId));

        List<String> chunks = chunker.chunk(page.getContent());

        int index = 0;
        List<TextChunk> savedChunks = new java.util.ArrayList<>();
        for (String content : chunks) {
            TextChunk chunk = new TextChunk();
            chunk.setRawNoteId(rawNoteId);
            chunk.setChunkIndex(index++);
            chunk.setContent(content);
            savedChunks.add(chunkRepo.save(chunk));
        }

        List<List<Double>> embeddings = aimlEmbeddingClient.buildEmbeddings(
                savedChunks.stream().map(TextChunk::getContent).toList()
        );

        int paired = Math.min(savedChunks.size(), embeddings.size());
        for (int i = 0; i < paired; i++) {
            vectorStoreService.upsertChunkEmbedding(savedChunks.get(i), embeddings.get(i));
        }
    }
}

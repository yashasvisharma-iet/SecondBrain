package com.example.demo.service;

import com.example.demo.entity.NotionPageContent;
import com.example.demo.entity.TextChunk;
import com.example.demo.repository.NotionPageContentRepository;
import com.example.demo.repository.TextChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChunkingService {

    private static final Logger log = LoggerFactory.getLogger(ChunkingService.class);

    private final NotionPageContentRepository pageRepo;
    private final TextChunkRepository chunkRepo;
    private final TextChunker chunker;
    private final AimlEmbeddingClient aimlEmbeddingClient;
    private final PineconeVectorStoreService vectorStoreService;

    public ChunkingService(
            NotionPageContentRepository pageRepo,
            TextChunkRepository chunkRepo,
            TextChunker chunker,
            AimlEmbeddingClient aimlEmbeddingClient,
            PineconeVectorStoreService vectorStoreService
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

        resetChunks(rawNoteId);

        NotionPageContent page = findPage(rawNoteId);
        List<TextChunk> chunks = createAndSaveChunks(page);

        List<List<Double>> embeddings = generateEmbeddings(chunks);
        upsertEmbeddings(rawNoteId, chunks, embeddings);

        logSuccess(rawNoteId, chunks.size(), embeddings.size());
    }

    private void resetChunks(Long rawNoteId) {
        chunkRepo.deleteAllByRawNoteId(rawNoteId);
        vectorStoreService.deleteByRawNoteId(rawNoteId);
    }

    private NotionPageContent findPage(Long rawNoteId) {
        return pageRepo.findById(rawNoteId)
                .orElseThrow(() -> new IllegalArgumentException("NotionPageContent not found: " + rawNoteId));
    }
    
    private List<TextChunk> createAndSaveChunks(NotionPageContent page) {
        List<String> contents = chunker.chunk(page.getContent());

        List<TextChunk> saved = new java.util.ArrayList<>();
        int index = 0;

        for (String content : contents) {
            saved.add(saveChunk(page, content, index++));
        }

        return saved;
    }

    private TextChunk saveChunk(NotionPageContent page, String content, int index) {
        TextChunk chunk = new TextChunk();
        chunk.setRawNoteId(page.getId());
        chunk.setAppUserId(page.getAppUserId());
        chunk.setChunkIndex(index);
        chunk.setContent(content);

        return chunkRepo.save(chunk);
    }

    private List<List<Double>> generateEmbeddings(List<TextChunk> chunks) {
        List<String> contents = chunks.stream()
                .map(TextChunk::getContent)
                .toList();

        return aimlEmbeddingClient.buildEmbeddings(contents);
    }

    private void upsertEmbeddings(Long rawNoteId, List<TextChunk> chunks, List<List<Double>> embeddings) {
        int paired = Math.min(chunks.size(), embeddings.size());

        logIfMismatch(rawNoteId, chunks.size(), paired);

        for (int i = 0; i < paired; i++) {
            vectorStoreService.upsertChunkEmbedding(chunks.get(i), embeddings.get(i));
        }
    }

    private void logIfMismatch(Long rawNoteId, int total, int paired) {
        if (paired < total) {
            log.warn("Only {} embedding(s) generated for {} chunk(s) of rawNoteId={}.",
                    paired, total, rawNoteId);
        }
    }

    private void logSuccess(Long rawNoteId, int chunks, int embeddings) {
        log.info("Processed {} chunk(s) for rawNoteId={} and upserted {} embeddings.",
                chunks, rawNoteId, embeddings);
    }


}

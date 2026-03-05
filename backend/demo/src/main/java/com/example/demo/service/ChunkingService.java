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
        // Always remove existing chunks for this raw note and re-chunk based on current content.
        // The delete is executed and flushed immediately to prevent unique-key collisions when
        // inserting a fresh (raw_note_id, chunk_index) sequence in the same transaction.
        chunkRepo.deleteAllByRawNoteId(rawNoteId);
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
        if (paired < savedChunks.size()) {
            log.warn("Only {} embedding(s) generated for {} chunk(s) of rawNoteId={}. Some chunks were not upserted to Pinecone.",
                    paired, savedChunks.size(), rawNoteId);
        }
        for (int i = 0; i < paired; i++) {
            vectorStoreService.upsertChunkEmbedding(savedChunks.get(i), embeddings.get(i));
        }

        log.info("Processed {} chunk(s) for rawNoteId={} and attempted Pinecone upsert for {} embedding(s).",
                savedChunks.size(), rawNoteId, paired);
    }
}

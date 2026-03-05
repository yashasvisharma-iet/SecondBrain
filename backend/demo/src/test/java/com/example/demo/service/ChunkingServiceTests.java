package com.example.demo.service;

import com.example.demo.entity.NotionPageContent;
import com.example.demo.entity.TextChunk;
import com.example.demo.repository.NotionPageContentRepository;
import com.example.demo.repository.TextChunkRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChunkingServiceTests {

    @Test
    void chunkNoteUpsertsEmbeddingForEachSavedChunk() {
        NotionPageContentRepository pageRepo = mock(NotionPageContentRepository.class);
        TextChunkRepository chunkRepo = mock(TextChunkRepository.class);
        TextChunker chunker = mock(TextChunker.class);
        AimlEmbeddingClient embeddingClient = mock(AimlEmbeddingClient.class);
        PineconeVectorStoreService vectorStore = mock(PineconeVectorStoreService.class);

        ChunkingService service = new ChunkingService(pageRepo, chunkRepo, chunker, embeddingClient, vectorStore);

        NotionPageContent page = new NotionPageContent();
        page.setId(42L);
        page.setContent("First. Second.");

        when(pageRepo.findById(42L)).thenReturn(Optional.of(page));
        when(chunkRepo.existsByRawNoteId(42L)).thenReturn(false);
        when(chunker.chunk("First. Second.")).thenReturn(List.of("first chunk", "second chunk"));

        TextChunk firstSaved = new TextChunk();
        firstSaved.setId(100L);
        firstSaved.setRawNoteId(42L);
        firstSaved.setChunkIndex(0);
        firstSaved.setContent("first chunk");

        TextChunk secondSaved = new TextChunk();
        secondSaved.setId(101L);
        secondSaved.setRawNoteId(42L);
        secondSaved.setChunkIndex(1);
        secondSaved.setContent("second chunk");

        when(chunkRepo.save(any(TextChunk.class))).thenReturn(firstSaved, secondSaved);
        when(embeddingClient.buildEmbeddings(List.of("first chunk", "second chunk")))
                .thenReturn(List.of(List.of(0.1, 0.2), List.of(0.3, 0.4)));

        service.chunkNote(42L);

        verify(vectorStore, times(1)).deleteByRawNoteId(42L);
        verify(vectorStore, times(1)).upsertChunkEmbedding(firstSaved, List.of(0.1, 0.2));
        verify(vectorStore, times(1)).upsertChunkEmbedding(secondSaved, List.of(0.3, 0.4));
    }

    @Test
    void chunkNoteSkipsExtraChunksWhenEmbeddingCountIsLower() {
        NotionPageContentRepository pageRepo = mock(NotionPageContentRepository.class);
        TextChunkRepository chunkRepo = mock(TextChunkRepository.class);
        TextChunker chunker = mock(TextChunker.class);
        AimlEmbeddingClient embeddingClient = mock(AimlEmbeddingClient.class);
        PineconeVectorStoreService vectorStore = mock(PineconeVectorStoreService.class);

        ChunkingService service = new ChunkingService(pageRepo, chunkRepo, chunker, embeddingClient, vectorStore);

        NotionPageContent page = new NotionPageContent();
        page.setId(7L);
        page.setContent("A B");

        when(pageRepo.findById(7L)).thenReturn(Optional.of(page));
        when(chunkRepo.existsByRawNoteId(7L)).thenReturn(false);
        when(chunker.chunk("A B")).thenReturn(List.of("a", "b"));

        TextChunk firstSaved = new TextChunk();
        firstSaved.setId(200L);
        firstSaved.setRawNoteId(7L);
        firstSaved.setChunkIndex(0);
        firstSaved.setContent("a");

        TextChunk secondSaved = new TextChunk();
        secondSaved.setId(201L);
        secondSaved.setRawNoteId(7L);
        secondSaved.setChunkIndex(1);
        secondSaved.setContent("b");

        when(chunkRepo.save(any(TextChunk.class))).thenReturn(firstSaved, secondSaved);
        when(embeddingClient.buildEmbeddings(List.of("a", "b"))).thenReturn(List.of(List.of(0.5, 0.6)));

        service.chunkNote(7L);

        verify(vectorStore, times(1)).upsertChunkEmbedding(firstSaved, List.of(0.5, 0.6));
        verify(vectorStore, never()).upsertChunkEmbedding(secondSaved, List.of(0.5, 0.6));
    }
}

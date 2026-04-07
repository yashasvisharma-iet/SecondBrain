package com.example.demo.service;

import com.example.demo.entity.NotionPageContent;
import com.example.demo.entity.TextChunk;
import com.example.demo.repository.NotionPageContentRepository;
import com.example.demo.repository.TextChunkRepository;
import com.example.demo.service.chunkingAndEmbedding.AimlEmbeddingClient;
import com.example.demo.service.chunkingAndEmbedding.ChunkingService;
import com.example.demo.service.chunkingAndEmbedding.PineconeVectorStoreService;
import com.example.demo.service.chunkingAndEmbedding.TextChunker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChunkingServiceTest {

    @Mock
    private NotionPageContentRepository pageRepo;
    @Mock
    private TextChunkRepository chunkRepo;
    @Mock
    private TextChunker chunker;
    @Mock
    private AimlEmbeddingClient embeddingClient;
    @Mock
    private PineconeVectorStoreService vectorStore;

    @InjectMocks
    private ChunkingService service;

    @Test
    void chunkNoteUpsertsEmbeddingForEachSavedChunk() {
        NotionPageContent page = new NotionPageContent("page-1", 2L, "First. Second.");
        setId(page, 42L);
        when(pageRepo.findById(42L)).thenReturn(Optional.of(page));
        when(chunker.chunk("First. Second.")).thenReturn(List.of("first chunk", "second chunk"));

        TextChunk firstSaved = savedChunk(100L, 42L, 2L, 0, "first chunk");
        TextChunk secondSaved = savedChunk(101L, 42L, 2L, 1, "second chunk");
        when(chunkRepo.save(any(TextChunk.class))).thenReturn(firstSaved, secondSaved);
        when(embeddingClient.buildEmbeddings(List.of("first chunk", "second chunk")))
                .thenReturn(List.of(List.of(0.1, 0.2), List.of(0.3, 0.4)));

        service.chunkNote(42L);

        verify(chunkRepo).deleteAllByRawNoteId(42L);
        verify(vectorStore).deleteByRawNoteId(42L);
        verify(vectorStore).upsertChunkEmbedding(firstSaved, List.of(0.1, 0.2));
        verify(vectorStore).upsertChunkEmbedding(secondSaved, List.of(0.3, 0.4));
    }

    @Test
    void chunkNoteSkipsExtraChunksWhenEmbeddingCountIsLower() {
        NotionPageContent page = new NotionPageContent("page-2", 3L, "A B");
        setId(page, 7L);
        when(pageRepo.findById(7L)).thenReturn(Optional.of(page));
        when(chunker.chunk("A B")).thenReturn(List.of("a", "b"));

        TextChunk firstSaved = savedChunk(200L, 7L, 3L, 0, "a");
        TextChunk secondSaved = savedChunk(201L, 7L, 3L, 1, "b");
        when(chunkRepo.save(any(TextChunk.class))).thenReturn(firstSaved, secondSaved);
        when(embeddingClient.buildEmbeddings(List.of("a", "b"))).thenReturn(List.of(List.of(0.5, 0.6)));

        service.chunkNote(7L);

        verify(vectorStore).upsertChunkEmbedding(firstSaved, List.of(0.5, 0.6));
        verify(vectorStore, never()).upsertChunkEmbedding(secondSaved, List.of(0.5, 0.6));
    }

    @Test
    void chunkNoteReturnsImmediatelyWhenRawNoteIdIsNull() {
        service.chunkNote(null);

        verify(chunkRepo, never()).deleteAllByRawNoteId(any());
        verify(pageRepo, never()).findById(any());
    }

    @Test
    void chunkNoteThrowsWhenPageIsMissing() {
        when(pageRepo.findById(55L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.chunkNote(55L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("NotionPageContent not found");
    }

    private TextChunk savedChunk(Long id, Long rawNoteId, Long appUserId, int chunkIndex, String content) {
        TextChunk chunk = new TextChunk(id, rawNoteId, chunkIndex, content);
        chunk.setAppUserId(appUserId);
        return chunk;
    }

    private void setId(NotionPageContent page, Long id) {
        try {
            var field = NotionPageContent.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(page, id);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}

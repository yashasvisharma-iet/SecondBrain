package com.example.demo;

import com.example.demo.dto.GraphDataDto;
import com.example.demo.dto.GraphEdgeDto;
import com.example.demo.entity.NotionPageContent;
import com.example.demo.entity.TextChunk;
import com.example.demo.repository.NotionPageContentRepository;
import com.example.demo.repository.TextChunkRepository;
import com.example.demo.service.GraphService;
import com.example.demo.service.PineconeVectorStoreService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GraphServiceTests {

    @Test
    void buildsOneChunkNodePerNoteAndIncludesOrphanChunkNotes() {
        NotionPageContentRepository pageRepo = mock(NotionPageContentRepository.class);
        TextChunkRepository chunkRepo = mock(TextChunkRepository.class);
        PineconeVectorStoreService vectorStore = mock(PineconeVectorStoreService.class);

        NotionPageContent note1 = new NotionPageContent("n1", "Note one title\nBody");
        NotionPageContent note2 = new NotionPageContent("n2", "Note two title\nBody");

        setId(note1, 1L);
        setId(note2, 2L);

        TextChunk c1 = new TextChunk(10L, 1L, 0, "AI helps take better notes");
        TextChunk c1Second = new TextChunk(12L, 1L, 1, "Second chunk that should not be rendered as an extra node");
        TextChunk c2 = new TextChunk(11L, 2L, 0, "Using AI for semantic links");
        TextChunk orphan = new TextChunk(13L, 99L, 0, "Chunk exists without matching page");

        when(pageRepo.findAllByOrderBySyncedAtDesc()).thenReturn(List.of(note1, note2));
        when(chunkRepo.findAllByOrderByRawNoteIdAscChunkIndexAsc()).thenReturn(List.of(c1, c1Second, c2, orphan));
        when(vectorStore.buildSemanticEdges(any(), eq(0.8))).thenReturn(List.of(new GraphEdgeDto("c-10", "c-11", 0.91)));

        GraphService graphService = new GraphService(pageRepo, chunkRepo, vectorStore, 0.8);
        GraphDataDto data = graphService.getFeedGraph();

        assertThat(data.nodes()).filteredOn(node -> node.type().equals("note")).hasSize(3);
        assertThat(data.nodes()).filteredOn(node -> node.type().equals("chunk")).hasSize(3);

        assertThat(data.edges()).anyMatch(e -> e.score() == null && e.source().equals("n1") && e.target().equals("c-10"));
        assertThat(data.edges()).anyMatch(e -> e.score() == null && e.source().equals("orphan-note-99") && e.target().equals("c-13"));
        assertThat(data.edges()).noneMatch(e -> e.score() == null && e.target().equals("c-12"));
        assertThat(data.edges()).anyMatch(e -> e.score() != null && e.score() >= 0.8);
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

package com.example.demo;

import com.example.demo.dto.GraphDataDto;
import com.example.demo.dto.GraphEdgeDto;
import com.example.demo.entity.NotionPageContent;
import com.example.demo.entity.TextChunk;
import com.example.demo.repository.NotionPageContentRepository;
import com.example.demo.repository.TextChunkRepository;
import com.example.demo.service.AimlEmbeddingClient;
import com.example.demo.service.GraphService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GraphServiceTests {

    @Test
    void buildsFrontendCompatibleGraphWithSemanticEdges() {
        NotionPageContentRepository pageRepo = mock(NotionPageContentRepository.class);
        TextChunkRepository chunkRepo = mock(TextChunkRepository.class);
        AimlEmbeddingClient aimlClient = mock(AimlEmbeddingClient.class);

        NotionPageContent note1 = new NotionPageContent("n1", "Note one title\nBody");
        NotionPageContent note2 = new NotionPageContent("n2", "Note two title\nBody");

        setId(note1, 1L);
        setId(note2, 2L);

        TextChunk c1 = new TextChunk(10L, 1L, 0, "AI helps take better notes");
        TextChunk c2 = new TextChunk(11L, 2L, 0, "Using AI for semantic links");

        when(pageRepo.findAllByOrderBySyncedAtDesc()).thenReturn(List.of(note1, note2));
        when(chunkRepo.findAllByOrderByRawNoteIdAscChunkIndexAsc()).thenReturn(List.of(c1, c2));
        when(aimlClient.buildSemanticEdges(any())).thenReturn(List.of(new GraphEdgeDto("c-10", "c-11", 0.91)));

        GraphService graphService = new GraphService(pageRepo, chunkRepo, aimlClient, 0.8);
        GraphDataDto data = graphService.getFeedGraph();

        assertThat(data.nodes()).extracting("type").contains("note", "chunk");
        assertThat(data.edges()).anyMatch(e -> e.score() == null && e.source().equals("n1") && e.target().equals("c-10"));
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

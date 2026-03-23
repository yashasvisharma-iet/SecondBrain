package com.example.demo.service;

import com.example.demo.dto.AgentQueryResponse;
import com.example.demo.dto.GraphDataDto;
import com.example.demo.dto.GraphEdgeDto;
import com.example.demo.entity.NotionPageContent;
import com.example.demo.entity.TextChunk;
import com.example.demo.repository.NotionPageContentRepository;
import com.example.demo.repository.TextChunkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GraphServiceTest {

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        graphService = new GraphService(pageRepo, chunkRepo, vectorStore, aimlEmbeddingClient, 0.45, 0.35);
    }

    @Mock
    private NotionPageContentRepository pageRepo;
    @Mock
    private TextChunkRepository chunkRepo;
    @Mock
    private PineconeVectorStoreService vectorStore;
    @Mock
    private AimlEmbeddingClient aimlEmbeddingClient;

    private GraphService graphService;

    @Test
    void buildsNoteOnlyGraphAndAggregatesSemanticEdgesAtNoteLevel() {
        NotionPageContent note1 = page(1L, "n1", 9L, "Study plan\nBody");
        NotionPageContent note2 = page(2L, "n2", 9L, "Family dinner\nBody");
        TextChunk c1 = chunk(10L, 1L, 9L, 0, "AI helps take better notes");
        TextChunk c1Second = chunk(12L, 1L, 9L, 1, "Second chunk that should not be rendered as an extra node");
        TextChunk c2 = chunk(11L, 2L, 9L, 0, "Using AI for semantic links");
        TextChunk orphan = chunk(13L, 99L, 9L, 0, "Chunk exists without matching page");

        when(pageRepo.findAllByAppUserIdOrderBySyncedAtDesc(9L)).thenReturn(List.of(note1, note2));
        when(chunkRepo.findAllByAppUserIdOrderByRawNoteIdAscChunkIndexAsc(9L)).thenReturn(List.of(c1, c1Second, c2, orphan));
        when(vectorStore.buildSemanticEdges(List.of(c1, c1Second, c2, orphan), 0.45)).thenReturn(List.of(
                new GraphEdgeDto("c-10", "c-11", 0.91),
                new GraphEdgeDto("c-12", "c-11", 0.87),
                new GraphEdgeDto("c-13", "c-11", 0.79)
        ));

        GraphDataDto data = graphService.getFeedGraph(9L);

        assertThat(data.nodes()).hasSize(3);
        assertThat(data.edges()).hasSize(2);
        assertThat(data.nodes()).extracting(node -> node.id()).containsExactlyInAnyOrder("n1", "n2", "orphan-note-99");
        assertThat(data.nodes()).extracting(node -> node.genre()).contains("Career", "Family", "Life");
        assertThat(data.edges()).anyMatch(e -> e.source().equals("n1") && e.target().equals("n2") && e.score().equals(0.91));
        assertThat(data.edges()).anyMatch(e -> e.source().equals("orphan-note-99") && e.target().equals("n2") && e.score().equals(0.79));
    }

    @Test
    void answerFromDatabaseUsesVectorRetrievalWhenAvailable() {
        NotionPageContent note = page(1L, "gdoc:page-1", 4L, "Vector note");
        when(aimlEmbeddingClient.buildEmbeddings(List.of("what is retrieval augmented generation")))
                .thenReturn(List.of(List.of(0.2, 0.3, 0.4)));
        when(vectorStore.querySimilarChunks(List.of(0.2, 0.3, 0.4), 5, 0.35, 4L))
                .thenReturn(List.of(new PineconeVectorStoreService.RetrievedChunkMatch(1L, 4L, 2,
                        "RAG fetches semantically similar chunks from vector stores before generation.", 99L, 0.88)));
        when(pageRepo.findById(1L)).thenReturn(Optional.of(note));

        AgentQueryResponse response = graphService.answerFromDatabase(4L, "what is retrieval augmented generation");

        assertThat(response.answer()).contains("relevant note chunk");
        assertThat(response.citations()).hasSize(1);
        assertThat(response.citations().get(0).pageId()).isEqualTo("gdoc:page-1");
        assertThat(response.citations().get(0).chunkIndex()).isEqualTo(2);
        assertThat(response.citations().get(0).source()).isEqualTo("Google Docs");
    }

    @Test
    void answerFromDatabaseFallsBackToKeywordSearchWhenVectorMisses() {
        NotionPageContent note = page(14L, "page-14", 14L, "Interview prep notes");
        TextChunk lexicalHit = chunk(47L, 14L, 14L, 0,
                "Attention-based neural networks (transformers) are strong for NLP tasks.");

        when(aimlEmbeddingClient.buildEmbeddings(List.of("transformers"))).thenReturn(List.of(List.of(0.2, 0.3, 0.4)));
        when(vectorStore.querySimilarChunks(List.of(0.2, 0.3, 0.4), 5, 0.35, 14L)).thenReturn(List.of());
        when(chunkRepo.searchByContent(14L, "transformers", PageRequest.of(0, 5))).thenReturn(List.of(lexicalHit));
        when(pageRepo.findById(14L)).thenReturn(Optional.of(note));

        AgentQueryResponse response = graphService.answerFromDatabase(14L, "transformers");

        assertThat(response.answer()).contains("keyword match");
        assertThat(response.citations()).hasSize(1);
        assertThat(response.citations().get(0).snippet()).contains("transformers");
    }

    @Test
    void summarizePageUsesAiSummaryWhenPageExistsAndFallbackOtherwise() {
        NotionPageContent note = page(14L, "page-14", 14L, "Long note content");
        when(pageRepo.findByPageIdAndAppUserId("page-14", 14L)).thenReturn(Optional.of(note));
        when(pageRepo.findByPageIdAndAppUserId("missing", 14L)).thenReturn(Optional.empty());
        when(aimlEmbeddingClient.summarizeText("Long note content")).thenReturn("Summarized");

        assertThat(graphService.summarizePage(14L, "page-14")).isEqualTo("Summarized");
        assertThat(graphService.summarizePage(14L, "missing")).contains("couldn't find");
        assertThat(graphService.summarizePage(14L, " ")).contains("Please select");
    }

    private NotionPageContent page(Long id, String pageId, Long appUserId, String content) {
        NotionPageContent page = new NotionPageContent(pageId, appUserId, content);
        page.setSyncedAt(Instant.parse("2024-01-01T00:00:00Z"));
        try {
            var field = NotionPageContent.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(page, id);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return page;
    }

    private TextChunk chunk(Long id, Long rawNoteId, Long appUserId, int index, String content) {
        TextChunk chunk = new TextChunk(id, rawNoteId, index, content);
        chunk.setAppUserId(appUserId);
        return chunk;
    }
}

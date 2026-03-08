package com.example.demo.service;

import com.example.demo.dto.GraphDataDto;
import com.example.demo.dto.GraphEdgeDto;
import com.example.demo.dto.GraphNodeDto;
import com.example.demo.dto.AgentQueryResponse;
import com.example.demo.entity.NotionPageContent;
import com.example.demo.entity.TextChunk;
import com.example.demo.repository.NotionPageContentRepository;
import com.example.demo.repository.TextChunkRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GraphService {

    private final NotionPageContentRepository pageRepository;
    private final TextChunkRepository chunkRepository;
    private final PineconeVectorStoreService vectorStoreService;
    private final AimlEmbeddingClient aimlEmbeddingClient;
    private final double threshold;

    public GraphService(NotionPageContentRepository pageRepository,
                        TextChunkRepository chunkRepository,
                        PineconeVectorStoreService vectorStoreService,
                        AimlEmbeddingClient aimlEmbeddingClient,
                        @Value("${semantic.threshold:0.7}") double threshold) {
        this.pageRepository = pageRepository;
        this.chunkRepository = chunkRepository;
        this.vectorStoreService = vectorStoreService;
        this.aimlEmbeddingClient = aimlEmbeddingClient;
        this.threshold = threshold;
    }

    public GraphDataDto getFeedGraph() {
        List<NotionPageContent> pages = pageRepository.findAllByOrderBySyncedAtDesc();
        List<TextChunk> chunks = chunkRepository.findAllByOrderByRawNoteIdAscChunkIndexAsc();

        Map<Long, NotionPageContent> pageById = pages.stream()
                .collect(Collectors.toMap(NotionPageContent::getId, Function.identity()));

        List<GraphNodeDto> nodes = new ArrayList<>();
        List<GraphEdgeDto> noteEdges = new ArrayList<>();

        HashSet<Long> seenRawNoteIds = new HashSet<>();
        for (TextChunk chunk : chunks) {
            Long rawNoteId = chunk.getRawNoteId();
            NotionPageContent page = pageById.get(rawNoteId);

            if (seenRawNoteIds.add(rawNoteId)) {
                String noteId = page != null ? noteNodeId(page) : fallbackNoteNodeId(rawNoteId);
                String noteLabel = page != null ? buildNoteLabel(page) : "Imported note " + rawNoteId;
                nodes.add(new GraphNodeDto(noteId, noteLabel, "note"));
            }
        }

        noteEdges.addAll(buildNoteSemanticEdges(chunks, pageById));

        return new GraphDataDto(nodes, noteEdges);
    }

    public AgentQueryResponse answerFromDatabase(String query) {
        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.isEmpty()) {
            return new AgentQueryResponse("Please enter a question for me to search your knowledge base.", List.of());
        }

        List<List<Double>> queryEmbeddings = aimlEmbeddingClient.buildEmbeddings(List.of(normalizedQuery));
        if (queryEmbeddings.isEmpty()) {
            return new AgentQueryResponse(
                    "I couldn't generate a query embedding right now, so vector retrieval is unavailable.",
                    List.of()
            );
        }

        List<PineconeVectorStoreService.RetrievedChunkMatch> matches = vectorStoreService
                .querySimilarChunks(queryEmbeddings.get(0), 5, 0.55);
        if (matches.isEmpty()) {
            return new AgentQueryResponse("I couldn't find matching notes in your vector database yet. Try different keywords or add more content.", List.of());
        }

        Map<Long, String> pageIdByRawNoteId = pageRepository.findAll().stream()
                .collect(Collectors.toMap(NotionPageContent::getId, NotionPageContent::getPageId));

        List<AgentQueryResponse.AgentCitationDto> citations = matches.stream()
                .sorted(Comparator.comparing(PineconeVectorStoreService.RetrievedChunkMatch::score).reversed())
                .map(match -> new AgentQueryResponse.AgentCitationDto(
                        pageIdByRawNoteId.getOrDefault(match.rawNoteId(), "orphan-note-" + match.rawNoteId()),
                        match.chunkIndex(),
                        abbreviate(match.content(), 180)))
                .toList();

        String answer = "I found " + matches.size() + " relevant chunk(s) in your vector database for: \"" + normalizedQuery + "\".";
        return new AgentQueryResponse(answer, citations);
    }

    private String abbreviate(String content, int maxChars) {
        if (content == null) {
            return "";
        }
        String compact = content.replaceAll("\\s+", " ").trim();
        if (compact.length() <= maxChars) {
            return compact;
        }
        return compact.substring(0, maxChars) + "...";
    }

    private List<GraphEdgeDto> buildNoteSemanticEdges(List<TextChunk> chunks, Map<Long, NotionPageContent> pageById) {
        Map<String, Long> rawNoteIdByChunkNodeId = chunks.stream()
                .filter(chunk -> chunk.getId() != null)
                .collect(Collectors.toMap(this::chunkNodeId, TextChunk::getRawNoteId));

        Map<String, GraphEdgeDto> deduplicatedNoteEdges = new HashMap<>();
        for (GraphEdgeDto chunkEdge : vectorStoreService.buildSemanticEdges(chunks, threshold)) {
            Long sourceRawNoteId = rawNoteIdByChunkNodeId.get(chunkEdge.source());
            Long targetRawNoteId = rawNoteIdByChunkNodeId.get(chunkEdge.target());
            if (sourceRawNoteId == null || targetRawNoteId == null || sourceRawNoteId.equals(targetRawNoteId)) {
                continue;
            }

            String sourceNoteId = resolveNoteNodeId(pageById.get(sourceRawNoteId), sourceRawNoteId);
            String targetNoteId = resolveNoteNodeId(pageById.get(targetRawNoteId), targetRawNoteId);
            String dedupKey = sourceNoteId.compareTo(targetNoteId) <= 0
                    ? sourceNoteId + "|" + targetNoteId
                    : targetNoteId + "|" + sourceNoteId;

            GraphEdgeDto existing = deduplicatedNoteEdges.get(dedupKey);
            if (existing == null || (chunkEdge.score() != null && chunkEdge.score() > existing.score())) {
                deduplicatedNoteEdges.put(dedupKey, new GraphEdgeDto(sourceNoteId, targetNoteId, chunkEdge.score()));
            }
        }

        return List.copyOf(deduplicatedNoteEdges.values());
    }

    private String noteNodeId(NotionPageContent page) {
        return page.getPageId();
    }

    private String chunkNodeId(TextChunk chunk) {
        return "c-" + chunk.getId();
    }

    private String fallbackNoteNodeId(Long rawNoteId) {
        return "orphan-note-" + rawNoteId;
    }

    private String resolveNoteNodeId(NotionPageContent page, Long rawNoteId) {
        return page != null ? noteNodeId(page) : fallbackNoteNodeId(rawNoteId);
    }

    private String buildNoteLabel(NotionPageContent page) {
        String content = page.getContent();
        if (content == null || content.isBlank()) {
            return "Untitled note";
        }

        String firstLine = content.lines().findFirst().orElse("Untitled note").trim();
        return firstLine.length() > 64 ? firstLine.substring(0, 64) + "..." : firstLine;
    }
}

package com.example.demo.service;

import com.example.demo.dto.GraphDataDto;
import com.example.demo.dto.GraphEdgeDto;
import com.example.demo.dto.GraphNodeDto;
import com.example.demo.entity.NotionPageContent;
import com.example.demo.entity.TextChunk;
import com.example.demo.repository.NotionPageContentRepository;
import com.example.demo.repository.TextChunkRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GraphService {

    private final NotionPageContentRepository pageRepository;
    private final TextChunkRepository chunkRepository;
    private final PineconeVectorStoreService vectorStoreService;
    private final double threshold;

    public GraphService(NotionPageContentRepository pageRepository,
                        TextChunkRepository chunkRepository,
                        PineconeVectorStoreService vectorStoreService,
                        @Value("${semantic.threshold:0.8}") double threshold) {
        this.pageRepository = pageRepository;
        this.chunkRepository = chunkRepository;
        this.vectorStoreService = vectorStoreService;
        this.threshold = threshold;
    }

    public GraphDataDto getFeedGraph() {
        List<NotionPageContent> pages = pageRepository.findAllByOrderBySyncedAtDesc();
        List<TextChunk> chunks = chunkRepository.findAllByOrderByRawNoteIdAscChunkIndexAsc();

        Map<Long, NotionPageContent> pageById = pages.stream()
                .collect(Collectors.toMap(NotionPageContent::getId, Function.identity()));

        List<GraphNodeDto> nodes = new ArrayList<>();
        List<GraphEdgeDto> edges = new ArrayList<>();

        Map<Long, TextChunk> firstChunkByRawNoteId = chunks.stream()
                .collect(Collectors.toMap(
                        TextChunk::getRawNoteId,
                        Function.identity(),
                        BinaryOperator.minBy(Comparator.comparingInt(TextChunk::getChunkIndex))
                ));

        List<TextChunk> graphChunks = new ArrayList<>();
        for (Map.Entry<Long, TextChunk> entry : firstChunkByRawNoteId.entrySet()) {
            Long rawNoteId = entry.getKey();
            TextChunk chunk = entry.getValue();
            NotionPageContent page = pageById.get(rawNoteId);

            String noteId = page != null ? noteNodeId(page) : fallbackNoteNodeId(rawNoteId);
            String noteLabel = page != null ? buildNoteLabel(page) : "Imported note " + rawNoteId;

            nodes.add(new GraphNodeDto(noteId, noteLabel, "note"));

            String chunkId = chunkNodeId(chunk);
            nodes.add(new GraphNodeDto(chunkId, "Chunk " + chunk.getChunkIndex(), "chunk"));
            edges.add(new GraphEdgeDto(noteId, chunkId, null));
            graphChunks.add(chunk);
        }

        edges.addAll(vectorStoreService.buildSemanticEdges(graphChunks, threshold));

        return new GraphDataDto(nodes, edges);
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

    private String buildNoteLabel(NotionPageContent page) {
        String content = page.getContent();
        if (content == null || content.isBlank()) {
            return "Untitled note";
        }

        String firstLine = content.lines().findFirst().orElse("Untitled note").trim();
        return firstLine.length() > 64 ? firstLine.substring(0, 64) + "..." : firstLine;
    }
}

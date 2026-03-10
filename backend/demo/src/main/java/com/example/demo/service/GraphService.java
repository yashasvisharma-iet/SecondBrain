package com.example.demo.service;

import com.example.demo.dto.AgentQueryResponse;
import com.example.demo.dto.GraphDataDto;
import com.example.demo.dto.GraphEdgeDto;
import com.example.demo.dto.GraphNodeDto;
import com.example.demo.entity.NotionPageContent;
import com.example.demo.entity.TextChunk;
import com.example.demo.repository.NotionPageContentRepository;
import com.example.demo.repository.TextChunkRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GraphService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.systemDefault());

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "from", "had", "has", "have",
            "he", "her", "his", "i", "if", "in", "into", "is", "it", "its", "me", "my", "not", "of", "on",
            "or", "our", "she", "so", "that", "the", "their", "them", "there", "they", "this", "to", "was",
            "we", "were", "with", "you", "your"
    );

    private final NotionPageContentRepository pageRepository;
    private final TextChunkRepository chunkRepository;
    private final PineconeVectorStoreService vectorStoreService;
    private final AimlEmbeddingClient aimlEmbeddingClient;
    private final double threshold;
    private final double queryMinScore;

    public GraphService(NotionPageContentRepository pageRepository,
                        TextChunkRepository chunkRepository,
                        PineconeVectorStoreService vectorStoreService,
                        AimlEmbeddingClient aimlEmbeddingClient,
                        @Value("${semantic.threshold:0.7}") double threshold,
                        @Value("${semantic.query-min-score:0.35}") double queryMinScore) {
        this.pageRepository = pageRepository;
        this.chunkRepository = chunkRepository;
        this.vectorStoreService = vectorStoreService;
        this.aimlEmbeddingClient = aimlEmbeddingClient;
        this.threshold = threshold;
        this.queryMinScore = queryMinScore;
    }

    public GraphDataDto getFeedGraph() {
        List<NotionPageContent> pages = pageRepository.findAllByOrderBySyncedAtDesc();
        List<TextChunk> chunks = chunkRepository.findAllByOrderByRawNoteIdAscChunkIndexAsc();

        Map<Long, NotionPageContent> pageById = pages.stream()
                .collect(Collectors.toMap(NotionPageContent::getId, Function.identity()));

        List<GraphNodeDto> nodes = new ArrayList<>();
        List<GraphEdgeDto> noteEdges = new ArrayList<>();
        List<GraphEdgeDto> topicEdges = new ArrayList<>();
        Map<String, String> noteTopicByNodeId = new HashMap<>();
        Set<String> topicIds = new LinkedHashSet<>();

        HashSet<Long> seenRawNoteIds = new HashSet<>();
        for (TextChunk chunk : chunks) {
            Long rawNoteId = chunk.getRawNoteId();
            NotionPageContent page = pageById.get(rawNoteId);

            if (seenRawNoteIds.add(rawNoteId)) {
                String noteId = page != null ? noteNodeId(page) : fallbackNoteNodeId(rawNoteId);
                String noteLabel = page != null ? buildNoteLabel(page) : "Imported note " + rawNoteId;
                nodes.add(new GraphNodeDto(noteId, noteLabel, "note"));

                String topicLabel = inferTopicLabel(page, chunksForRawNote(chunks, rawNoteId));
                String topicId = topicNodeId(topicLabel);
                noteTopicByNodeId.put(noteId, topicId);
                topicIds.add(topicId + "::" + topicLabel);
                topicEdges.add(new GraphEdgeDto(topicId, noteId, 1.0));
            }
        }

        for (String topic : topicIds) {
            String[] pieces = topic.split("::", 2);
            nodes.add(new GraphNodeDto(pieces[0], pieces[1], "topic"));
        }

        noteEdges.addAll(buildNoteSemanticEdges(chunks, pageById));
        if (noteEdges.isEmpty()) {
            noteEdges.addAll(buildFallbackLexicalEdges(chunks, pageById));
        }

        List<GraphEdgeDto> allEdges = new ArrayList<>(topicEdges);
        allEdges.addAll(noteEdges);

        if (noteEdges.isEmpty()) {
            allEdges.addAll(connectTopicsBySharedContext(noteTopicByNodeId));
        }

        return new GraphDataDto(nodes, allEdges);
    }

    private List<TextChunk> chunksForRawNote(List<TextChunk> chunks, Long rawNoteId) {
        return chunks.stream()
                .filter(chunk -> rawNoteId.equals(chunk.getRawNoteId()))
                .toList();
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
                .querySimilarChunks(queryEmbeddings.get(0), 5, queryMinScore);

        if (!matches.isEmpty()) {
            List<AgentQueryResponse.AgentCitationDto> citations = matches.stream()
                    .sorted(Comparator.comparing(PineconeVectorStoreService.RetrievedChunkMatch::score).reversed())
                    .map(match -> toCitation(
                            pageRepository.findById(match.rawNoteId()).orElse(null),
                            match.chunkIndex(),
                            abbreviate(match.content(), 360)))
                    .toList();

            String answer = "Yes — I found " + matches.size() + " relevant note chunk(s) for \"" + normalizedQuery
                    + "\". Here are the matching sayings with source and date.";
            return new AgentQueryResponse(answer, citations);
        }

        List<TextChunk> lexicalMatches = chunkRepository.searchByContent(normalizedQuery, PageRequest.of(0, 5));
        if (!lexicalMatches.isEmpty()) {
            List<AgentQueryResponse.AgentCitationDto> citations = lexicalMatches.stream()
                    .map(chunk -> toCitation(
                            pageRepository.findById(chunk.getRawNoteId()).orElse(null),
                            chunk.getChunkIndex(),
                            abbreviate(chunk.getContent(), 360)))
                    .toList();

            String answer = "I didn't get strong vector matches, but I found " + lexicalMatches.size()
                    + " keyword match(es) in your notes for: \"" + normalizedQuery + "\".";
            return new AgentQueryResponse(answer, citations);
        }

        return new AgentQueryResponse(
                "I couldn't find matching notes in either vector search or keyword search yet. Try different keywords or add more content.",
                List.of());
    }

    public String summarizePage(String pageId) {
        if (pageId == null || pageId.isBlank()) {
            return "Please select a note to summarize.";
        }

        return pageRepository.findByPageId(pageId)
                .map(page -> aimlEmbeddingClient.summarizeText(page.getContent()))
                .orElse("I couldn't find that note in the database yet.");
    }

    private AgentQueryResponse.AgentCitationDto toCitation(NotionPageContent page, Integer chunkIndex, String snippet) {
        String pageId = page != null ? page.getPageId() : "unknown-source";
        return new AgentQueryResponse.AgentCitationDto(
                pageId,
                chunkIndex,
                snippet,
                detectSource(pageId),
                formatSyncedDate(page));
    }

    private String detectSource(String pageId) {
        if (pageId != null && pageId.startsWith("gdoc:")) {
            return "Google Docs";
        }
        return "Notion";
    }

    private String formatSyncedDate(NotionPageContent page) {
        if (page == null || page.getSyncedAt() == null) {
            return "unknown date";
        }
        return DATE_FORMATTER.format(page.getSyncedAt());
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

    private String inferTopicLabel(NotionPageContent page, List<TextChunk> noteChunks) {
        String combined = new StringBuilder()
                .append(page != null && page.getContent() != null ? page.getContent() : "")
                .append(" ")
                .append(noteChunks.stream().map(TextChunk::getContent).collect(Collectors.joining(" ")))
                .toString()
                .toLowerCase();

        if (combined.contains("internship") || combined.contains("study") || combined.contains("exam")
                || combined.contains("project") || combined.contains("presentation")) {
            return "Career & Study";
        }
        if (combined.contains("mom") || combined.contains("dad") || combined.contains("home")
                || combined.contains("family") || combined.contains("parents")) {
            return "Family & Safety";
        }
        if (combined.contains("friend") || combined.contains("relationship") || combined.contains("hangout")
                || combined.contains("love")) {
            return "Relationships";
        }
        if (combined.contains("everest") || combined.contains("travel") || combined.contains("trip")
                || combined.contains("hiking") || combined.contains("hobbies")) {
            return "Goals & Adventure";
        }

        return "General Reflection";
    }

    private String topicNodeId(String topicLabel) {
        return "topic:" + topicLabel.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }

    private List<GraphEdgeDto> buildFallbackLexicalEdges(List<TextChunk> chunks,
                                                          Map<Long, NotionPageContent> pageById) {
        Map<Long, String> noteText = chunks.stream()
                .collect(Collectors.groupingBy(TextChunk::getRawNoteId,
                        Collectors.mapping(TextChunk::getContent, Collectors.joining(" "))));

        List<Long> noteIds = new ArrayList<>(noteText.keySet());
        List<GraphEdgeDto> edges = new ArrayList<>();

        for (int i = 0; i < noteIds.size(); i++) {
            for (int j = i + 1; j < noteIds.size(); j++) {
                Long first = noteIds.get(i);
                Long second = noteIds.get(j);
                double score = lexicalSimilarity(noteText.get(first), noteText.get(second));
                if (score >= 0.12) {
                    edges.add(new GraphEdgeDto(
                            resolveNoteNodeId(pageById.get(first), first),
                            resolveNoteNodeId(pageById.get(second), second),
                            score
                    ));
                }
            }
        }

        return edges.stream()
                .sorted(Comparator.comparing((GraphEdgeDto edge) -> edge.score() != null ? edge.score() : 0.0).reversed())
                .limit(30)
                .toList();
    }

    private double lexicalSimilarity(String left, String right) {
        Set<String> leftTokens = tokenize(left);
        Set<String> rightTokens = tokenize(right);
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
            return 0;
        }

        Set<String> overlap = new HashSet<>(leftTokens);
        overlap.retainAll(rightTokens);
        if (overlap.isEmpty()) {
            return 0;
        }

        Set<String> union = new HashSet<>(leftTokens);
        union.addAll(rightTokens);
        return (double) overlap.size() / union.size();
    }

    private Set<String> tokenize(String content) {
        if (content == null || content.isBlank()) {
            return Set.of();
        }

        return Arrays.stream(content.toLowerCase().split("[^a-z0-9]+"))
                .filter(token -> token.length() > 2)
                .filter(token -> !STOP_WORDS.contains(token))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<GraphEdgeDto> connectTopicsBySharedContext(Map<String, String> noteTopicByNodeId) {
        Map<String, Long> counts = noteTopicByNodeId.values().stream()
                .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));
        List<String> topicIds = new ArrayList<>(counts.keySet());
        List<GraphEdgeDto> topicEdges = new ArrayList<>();

        for (int i = 0; i < topicIds.size(); i++) {
            for (int j = i + 1; j < topicIds.size(); j++) {
                String left = topicIds.get(i);
                String right = topicIds.get(j);
                double score = Math.min(counts.get(left), counts.get(right))
                        / (double) Math.max(counts.get(left), counts.get(right));
                topicEdges.add(new GraphEdgeDto(left, right, score));
            }
        }

        return topicEdges;
    }
}

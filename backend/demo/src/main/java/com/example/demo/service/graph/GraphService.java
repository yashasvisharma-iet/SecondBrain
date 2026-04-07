package com.example.demo.service.graph;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.example.demo.dto.AgentQueryResponse;
import com.example.demo.dto.GraphDataDto;
import com.example.demo.dto.GraphEdgeDto;
import com.example.demo.dto.GraphNodeDto;
import com.example.demo.entity.NotionPageContent;
import com.example.demo.entity.TextChunk;
import com.example.demo.repository.NotionPageContentRepository;
import com.example.demo.repository.TextChunkRepository;
import com.example.demo.service.chunkingAndEmbedding.AimlEmbeddingClient;
import com.example.demo.service.chunkingAndEmbedding.PineconeVectorStoreService;

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
                        @Value("${semantic.threshold:0.45}") double threshold,
                        @Value("${semantic.query-min-score:0.35}") double queryMinScore) {
        this.pageRepository = pageRepository;
        this.chunkRepository = chunkRepository;
        this.vectorStoreService = vectorStoreService;
        this.aimlEmbeddingClient = aimlEmbeddingClient;
        this.threshold = threshold;
        this.queryMinScore = queryMinScore;
    }

    public GraphDataDto getFeedGraph(Long appUserId) {
        List<NotionPageContent> pages = pageRepository.findAllByAppUserIdOrderBySyncedAtDesc(appUserId);
        List<TextChunk> chunks = chunkRepository.findAllByAppUserIdOrderByRawNoteIdAscChunkIndexAsc(appUserId);

        Map<Long, NotionPageContent> pageById = pages.stream()
                .collect(Collectors.toMap(NotionPageContent::getId, Function.identity()));

        List<GraphNodeDto> nodes = new ArrayList<>();
        Set<String> noteNodeIds = new LinkedHashSet<>();
        HashSet<Long> seenRawNoteIds = new HashSet<>();
        for (TextChunk chunk : chunks) {
            Long rawNoteId = chunk.getRawNoteId();
            NotionPageContent page = pageById.get(rawNoteId);

            if (seenRawNoteIds.add(rawNoteId)) {
                String noteId = page != null ? noteNodeId(page) : fallbackNoteNodeId(rawNoteId);
                String noteLabel = page != null ? buildNoteLabel(page) : "Imported note " + rawNoteId;
                String genre = inferGenreLabel(page, chunksForRawNote(chunks, rawNoteId));
                nodes.add(new GraphNodeDto(noteId, noteLabel, "note", genre));
                noteNodeIds.add(noteId);
            }
        }

        List<GraphEdgeDto> semanticEdges = buildNoteSemanticEdges(chunks, pageById);
        List<GraphEdgeDto> noteEdges = selectExploratoryEdges(semanticEdges, noteNodeIds, 3);

        if (noteEdges.isEmpty() && !noteNodeIds.isEmpty()) {
            List<GraphEdgeDto> lexicalEdges = buildFallbackLexicalEdges(chunks, pageById);
            noteEdges = selectExploratoryEdges(lexicalEdges, noteNodeIds, 3);
        }

        return new GraphDataDto(nodes, noteEdges);
    }

    private List<TextChunk> chunksForRawNote(List<TextChunk> chunks, Long rawNoteId) {
        return chunks.stream()
                .filter(chunk -> rawNoteId.equals(chunk.getRawNoteId()))
                .toList();
    }

    public AgentQueryResponse answerFromDatabase(Long appUserId, String query) {
        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.isEmpty()) {
            return new AgentQueryResponse("Please enter a question for me to search your knowledge base.", List.of());
        }

        List<List<Double>> queryEmbeddings = aimlEmbeddingClient.buildEmbeddings(List.of(normalizedQuery));
        boolean vectorAvailable = !queryEmbeddings.isEmpty();
        List<PineconeVectorStoreService.RetrievedChunkMatch> matches = vectorAvailable
                ? vectorStoreService.querySimilarChunks(queryEmbeddings.get(0), 5, queryMinScore, appUserId)
                : List.of();

        if (!matches.isEmpty()) {
            List<AgentQueryResponse.AgentCitationDto> citations = matches.stream()
                    .sorted(Comparator.comparing(PineconeVectorStoreService.RetrievedChunkMatch::score).reversed())
                    .map(match -> toCitation(
                            pageRepository.findById(match.rawNoteId())
                                    .filter(page -> appUserId.equals(page.getAppUserId()))
                                    .orElse(null),
                            match.chunkIndex(),
                            abbreviate(match.content(), 360)))
                    .toList();

            String answer = "Yes — I found " + matches.size() + " relevant note chunk(s) for \"" + normalizedQuery
                    + "\". Here are the matching sayings with source and date.";
            return new AgentQueryResponse(answer, citations);
        }

        List<TextChunk> lexicalMatches = chunkRepository.searchByContent(appUserId, normalizedQuery, PageRequest.of(0, 5));
        if (!lexicalMatches.isEmpty()) {
            List<AgentQueryResponse.AgentCitationDto> citations = lexicalMatches.stream()
                    .map(chunk -> toCitation(
                            pageRepository.findById(chunk.getRawNoteId())
                                    .filter(page -> appUserId.equals(page.getAppUserId()))
                                    .orElse(null),
                            chunk.getChunkIndex(),
                            abbreviate(chunk.getContent(), 360)))
                    .toList();

            String answer;
            if (vectorAvailable) {
                answer = "I didn't get strong vector matches, but I found " + lexicalMatches.size()
                        + " keyword match(es) in your notes for: \"" + normalizedQuery + "\".";
            } else {
                answer = "I found " + lexicalMatches.size()
                        + " keyword match(es) in your notes for: \"" + normalizedQuery + "\".";
            }
            return new AgentQueryResponse(answer, citations);
        }

        String answer = vectorAvailable
                ? "I couldn't find matching notes in either vector search or keyword search yet. Try different keywords or add more content."
                : "I couldn't find a direct match in your notes yet. Try different keywords, or add more detail so I can respond with stronger context.";
        return new AgentQueryResponse(answer, List.of());
    }

    public String summarizePage(Long appUserId, String pageId) {
        if (pageId == null || pageId.isBlank()) {
            return "Please select a note to summarize.";
        }

        return pageRepository.findByPageIdAndAppUserId(pageId, appUserId)
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
            String dedupKey = edgeKey(sourceNoteId, targetNoteId);

            GraphEdgeDto existing = deduplicatedNoteEdges.get(dedupKey);
            if (existing == null || (chunkEdge.score() != null && chunkEdge.score() > existing.score())) {
                deduplicatedNoteEdges.put(dedupKey, new GraphEdgeDto(sourceNoteId, targetNoteId, chunkEdge.score()));
            }
        }

        return List.copyOf(deduplicatedNoteEdges.values());
    }

    private List<GraphEdgeDto> selectExploratoryEdges(List<GraphEdgeDto> candidates,
                                                      Set<String> noteNodeIds,
                                                      int neighborsPerNote) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        Map<String, List<GraphEdgeDto>> byNode = new HashMap<>();
        for (String noteId : noteNodeIds) {
            byNode.put(noteId, new ArrayList<>());
        }

        for (GraphEdgeDto edge : candidates) {
            if (!byNode.containsKey(edge.source()) || !byNode.containsKey(edge.target())) {
                continue;
            }
            byNode.get(edge.source()).add(edge);
            byNode.get(edge.target()).add(edge);
        }

        Comparator<GraphEdgeDto> byScoreDesc = Comparator
                .comparing((GraphEdgeDto edge) -> edge.score() != null ? edge.score() : 0.0)
                .reversed();

        Set<String> selectedKeys = new LinkedHashSet<>();
        for (String noteId : noteNodeIds) {
            byNode.getOrDefault(noteId, List.of()).stream()
                    .sorted(byScoreDesc)
                    .limit(neighborsPerNote)
                    .forEach(edge -> selectedKeys.add(edgeKey(edge.source(), edge.target())));
        }

        return candidates.stream()
                .filter(edge -> selectedKeys.contains(edgeKey(edge.source(), edge.target())))
                .sorted(byScoreDesc)
                .toList();
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

    private String inferGenreLabel(NotionPageContent page, List<TextChunk> noteChunks) {
        String combined = new StringBuilder()
                .append(page != null && page.getContent() != null ? page.getContent() : "")
                .append(" ")
                .append(noteChunks.stream().map(TextChunk::getContent).collect(Collectors.joining(" ")))
                .toString()
                .toLowerCase();

        if (combined.contains("internship") || combined.contains("study") || combined.contains("exam")
                || combined.contains("project") || combined.contains("presentation")) {
            return "Career";
        }
        if (combined.contains("mom") || combined.contains("dad") || combined.contains("home")
                || combined.contains("family") || combined.contains("parents")) {
            return "Family";
        }
        if (combined.contains("friend") || combined.contains("relationship") || combined.contains("hangout")
                || combined.contains("love")) {
            return "Relationships";
        }
        if (combined.contains("everest") || combined.contains("travel") || combined.contains("trip")
                || combined.contains("hiking") || combined.contains("hobbies")) {
            return "Goals";
        }
        if (combined.contains("why") || combined.contains("meaning") || combined.contains("life")
                || combined.contains("think") || combined.contains("feel")) {
            return "Philosophy";
        }

        return "Life";
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

    private String edgeKey(String left, String right) {
        return left.compareTo(right) <= 0 ? left + "|" + right : right + "|" + left;
    }
}

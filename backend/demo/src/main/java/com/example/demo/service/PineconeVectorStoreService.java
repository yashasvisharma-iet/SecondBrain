package com.example.demo.service;

import com.example.demo.dto.GraphEdgeDto;
import com.example.demo.entity.TextChunk;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class PineconeVectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(PineconeVectorStoreService.class);

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String indexHost;
    private final String namespace;
    private final int dimensions;
    private final int queryTopK;

    public PineconeVectorStoreService(
            @Value("${pinecone.api-key:}") String apiKey,
            @Value("${pinecone.index-host:}") String indexHost,
            @Value("${pinecone.namespace:secondbrain}") String namespace,
            @Value("${vector.store.dimensions:1536}") int dimensions,
            @Value("${semantic.top-k:8}") int queryTopK
    ) {
        this.restTemplate = new RestTemplate();
        this.apiKey = apiKey;
        this.indexHost = indexHost;
        this.namespace = namespace;
        this.dimensions = dimensions;
        this.queryTopK = queryTopK;
    }

    public void deleteByRawNoteId(Long rawNoteId) {
        if (rawNoteId == null || !isConfigured()) {
            return;
        }

        Map<String, Object> body = Map.of(
                "namespace", namespace,
                "filter", Map.of("rawNoteId", Map.of("$eq", rawNoteId))
        );

        post("/vectors/delete", body, Void.class);
    }

    public void upsertChunkEmbedding(TextChunk chunk, List<Double> embedding) {
        if (chunk == null || chunk.getId() == null || embedding == null || embedding.isEmpty() || !isConfigured()) {
            return;
        }

        if (embedding.size() != dimensions) {
            throw new IllegalArgumentException("Embedding dimension mismatch. Expected " + dimensions + " got " + embedding.size());
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("chunkId", chunk.getId());
        metadata.put("rawNoteId", chunk.getRawNoteId());
        metadata.put("chunkIndex", chunk.getChunkIndex());
        metadata.put("content", chunk.getContent());

        Map<String, Object> vector = Map.of(
                "id", chunkVectorId(chunk.getId()),
                "values", embedding,
                "metadata", metadata
        );

        Map<String, Object> body = Map.of(
                "namespace", namespace,
                "vectors", List.of(vector)
        );

        post("/vectors/upsert", body, Void.class);
        log.info("Upserted embedding for chunk {} (rawNoteId={}, chunkIndex={}) into Pinecone namespace '{}'",
                chunk.getId(), chunk.getRawNoteId(), chunk.getChunkIndex(), namespace);
    }

    public List<GraphEdgeDto> buildSemanticEdges(List<TextChunk> chunks, double threshold) {
        if (chunks == null || chunks.isEmpty() || !isConfigured()) {
            return List.of();
        }

        Map<String, TextChunk> chunkByVectorId = chunks.stream()
                .filter(c -> c.getId() != null)
                .collect(java.util.stream.Collectors.toMap(c -> chunkVectorId(c.getId()), c -> c));

        Map<String, GraphEdgeDto> deduplicated = new HashMap<>();

        for (TextChunk sourceChunk : chunks) {
            if (sourceChunk.getId() == null || !isEligibleForSimilarity(sourceChunk.getContent())) {
                continue;
            }

            Map<String, Object> body = Map.of(
                    "namespace", namespace,
                    "id", chunkVectorId(sourceChunk.getId()),
                    "topK", queryTopK,
                    "includeMetadata", true
            );

            QueryResponse response = post("/query", body, QueryResponse.class);
            if (response == null || response.matches == null) {
                continue;
            }

            for (QueryMatch match : response.matches) {
                if (match == null || match.id == null || match.score == null) {
                    continue;
                }
                if (Objects.equals(match.id, chunkVectorId(sourceChunk.getId())) || match.score < threshold) {
                    continue;
                }

                TextChunk targetChunk = chunkByVectorId.get(match.id);
                if (targetChunk == null
                        || Objects.equals(targetChunk.getRawNoteId(), sourceChunk.getRawNoteId())
                        || !isEligibleForSimilarity(targetChunk.getContent())) {
                    continue;
                }

                String a = "c-" + sourceChunk.getId();
                String b = "c-" + targetChunk.getId();
                String key = a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;

                GraphEdgeDto existing = deduplicated.get(key);
                if (existing == null || match.score > existing.score()) {
                    deduplicated.put(key, new GraphEdgeDto(a, b, match.score));
                }
            }
        }

        return List.copyOf(deduplicated.values());
    }

    public List<RetrievedChunkMatch> querySimilarChunks(List<Double> queryEmbedding, int topK, double minScore) {
        if (queryEmbedding == null || queryEmbedding.isEmpty() || !isConfigured()) {
            return List.of();
        }

        if (queryEmbedding.size() != dimensions) {
            throw new IllegalArgumentException("Embedding dimension mismatch. Expected " + dimensions + " got " + queryEmbedding.size());
        }

        Map<String, Object> body = Map.of(
                "namespace", namespace,
                "vector", queryEmbedding,
                "topK", topK,
                "includeMetadata", true
        );

        QueryResponse response = post("/query", body, QueryResponse.class);
        if (response == null || response.matches == null) {
            return List.of();
        }

        return response.matches.stream()
                .filter(match -> match != null && match.score != null && match.metadata != null)
                .filter(match -> match.score >= minScore)
                .map(this::toRetrievedChunkMatch)
                .filter(Objects::nonNull)
                .toList();
    }


    private boolean isEligibleForSimilarity(String content) {
        if (content == null) {
            return false;
        }

        String normalized = content.trim();
        if (normalized.length() < 40) {
            return false;
        }

        String[] tokens = normalized.split("\\s+");
        if (tokens.length < 8) {
            return false;
        }

        long alphaNumericTokens = java.util.Arrays.stream(tokens)
                .map(token -> token.replaceAll("[^A-Za-z0-9]", ""))
                .filter(token -> token.length() >= 3)
                .count();

        return alphaNumericTokens >= 5;
    }

    private boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && indexHost != null && !indexHost.isBlank();
    }

    private String chunkVectorId(Long chunkId) {
        return "chunk-" + chunkId;
    }

    private <T> T post(String path, Object body, Class<T> responseType) {
        try {
            RequestEntity<Object> request = RequestEntity
                    .post(URI.create(indexHost + path))
                    .header("Api-Key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body);

            ResponseEntity<T> response = restTemplate.exchange(request, responseType);
            return response.getBody();
        } catch (RestClientException ex) {
            log.warn("Pinecone request failed for path {}: {}", path, ex.getMessage());
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class QueryResponse {
        @JsonProperty("matches")
        List<QueryMatch> matches;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class QueryMatch {
        @JsonProperty("id")
        String id;

        @JsonProperty("score")
        Double score;

        @JsonProperty("metadata")
        Map<String, Object> metadata;
    }

    private RetrievedChunkMatch toRetrievedChunkMatch(QueryMatch match) {
        Long rawNoteId = toLong(match.metadata.get("rawNoteId"));
        Integer chunkIndex = toInteger(match.metadata.get("chunkIndex"));
        String content = toStringValue(match.metadata.get("content"));
        Long chunkId = toLong(match.metadata.get("chunkId"));

        if (rawNoteId == null || chunkIndex == null || content == null) {
            return null;
        }

        return new RetrievedChunkMatch(rawNoteId, chunkIndex, content, chunkId, match.score);
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String toStringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    public record RetrievedChunkMatch(Long rawNoteId, Integer chunkIndex, String content, Long chunkId, Double score) {
    }
}

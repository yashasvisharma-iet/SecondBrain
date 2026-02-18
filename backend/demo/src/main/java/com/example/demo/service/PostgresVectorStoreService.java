package com.example.demo.service;

import com.example.demo.entity.TextChunk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class PostgresVectorStoreService {

    private final JdbcTemplate jdbcTemplate;
    private final String tableName;
    private final int dimensions;

    public PostgresVectorStoreService(
            JdbcTemplate jdbcTemplate,
            @Value("${vector.store.table:text_chunk_embedding}") String tableName,
            @Value("${vector.store.dimensions:384}") int dimensions
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.tableName = tableName;
        this.dimensions = dimensions;
    }

    @PostConstruct
    public void initialize() {
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s (
                    chunk_id BIGINT PRIMARY KEY,
                    raw_note_id BIGINT NOT NULL,
                    chunk_index INT NOT NULL,
                    content TEXT NOT NULL,
                    embedding vector(%d) NOT NULL,
                    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
                )
                """.formatted(tableName, dimensions));

        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS %s
                ON %s USING hnsw (embedding vector_cosine_ops)
                """.formatted(tableName + "_embedding_hnsw_idx", tableName));
    }

    public void deleteByRawNoteId(Long rawNoteId) {
        if (rawNoteId == null) {
            return;
        }

        jdbcTemplate.update("DELETE FROM " + tableName + " WHERE raw_note_id = ?", rawNoteId);
    }

    public void upsertChunkEmbedding(TextChunk chunk, List<Double> embedding) {
        if (chunk == null || chunk.getId() == null || embedding == null || embedding.isEmpty()) {
            return;
        }

        if (embedding.size() != dimensions) {
            throw new IllegalArgumentException("Embedding dimension mismatch. Expected " + dimensions + " got " + embedding.size());
        }

        String vectorLiteral = toVectorLiteral(embedding);
        jdbcTemplate.update("""
                INSERT INTO %s (chunk_id, raw_note_id, chunk_index, content, embedding, updated_at)
                VALUES (?, ?, ?, ?, CAST(? AS vector), NOW())
                ON CONFLICT (chunk_id)
                DO UPDATE SET
                    raw_note_id = EXCLUDED.raw_note_id,
                    chunk_index = EXCLUDED.chunk_index,
                    content = EXCLUDED.content,
                    embedding = EXCLUDED.embedding,
                    updated_at = NOW()
                """.formatted(tableName),
                chunk.getId(),
                chunk.getRawNoteId(),
                chunk.getChunkIndex(),
                chunk.getContent(),
                vectorLiteral
        );
    }

    private String toVectorLiteral(List<Double> embedding) {
        return embedding.stream()
                .map(value -> String.format(Locale.US, "%.8f", value))
                .collect(Collectors.joining(",", "[", "]"));
    }
}


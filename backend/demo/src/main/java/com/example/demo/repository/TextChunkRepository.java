package com.example.demo.repository;

import com.example.demo.entity.TextChunk;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TextChunkRepository extends JpaRepository<TextChunk, Long> {
    boolean existsByRawNoteId(Long rawNoteId);
    void deleteByRawNoteId(Long rawNoteId);
}

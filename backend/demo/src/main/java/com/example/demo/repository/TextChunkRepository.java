package com.example.demo.repository;

import com.example.demo.entity.TextChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TextChunkRepository extends JpaRepository<TextChunk, Long> {
    boolean existsByRawNoteId(Long rawNoteId);
    void deleteByRawNoteId(Long rawNoteId);
    List<TextChunk> findAllByOrderByRawNoteIdAscChunkIndexAsc();
}

package com.example.demo.repository;

import com.example.demo.entity.TextChunk;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TextChunkRepository extends JpaRepository<TextChunk, Long> {
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from TextChunk t where t.rawNoteId = :rawNoteId")
    void deleteAllByRawNoteId(@Param("rawNoteId") Long rawNoteId);
    List<TextChunk> findAllByOrderByRawNoteIdAscChunkIndexAsc();
}

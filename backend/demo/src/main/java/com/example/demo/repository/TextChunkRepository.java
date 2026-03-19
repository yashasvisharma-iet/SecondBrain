package com.example.demo.repository;

import com.example.demo.entity.TextChunk;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TextChunkRepository extends JpaRepository<TextChunk, Long> {
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from TextChunk t where t.rawNoteId = :rawNoteId")
    void deleteAllByRawNoteId(@Param("rawNoteId") Long rawNoteId);

    List<TextChunk> findAllByAppUserIdOrderByRawNoteIdAscChunkIndexAsc(Long appUserId);

    @Query("""
            select t from TextChunk t
            where t.appUserId = :appUserId
              and lower(t.content) like lower(concat('%', :query, '%'))
            order by t.rawNoteId asc, t.chunkIndex asc
            """)
    List<TextChunk> searchByContent(@Param("appUserId") Long appUserId,
                                    @Param("query") String query,
                                    Pageable pageable);
}

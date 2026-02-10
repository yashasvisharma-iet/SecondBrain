package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
  name = "text_chunk",
  uniqueConstraints = @UniqueConstraint(
    columnNames = {"raw_note_id", "chunk_index"}
  )
)
public class TextChunk {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "raw_note_id", nullable = false)
  private Long rawNoteId;

  @Column(name = "chunk_index", nullable = false)
  private int chunkIndex;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String content;
  
  public TextChunk() {}
  

  public TextChunk(Long id, Long rawNoteId, int chunkIndex, String content) {
    this.id = id;
    this.rawNoteId = rawNoteId;
    this.chunkIndex = chunkIndex;
    this.content = content;
}

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getRawNoteId() {
    return rawNoteId;
  }

  public void setRawNoteId(Long rawNoteId) {
    this.rawNoteId = rawNoteId;
  }

  public int getChunkIndex() {
    return chunkIndex;
  }

  public void setChunkIndex(int chunkIndex) {
    this.chunkIndex = chunkIndex;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  

  // getters/setters
}

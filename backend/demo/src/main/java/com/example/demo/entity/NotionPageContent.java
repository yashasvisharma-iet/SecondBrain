package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "notion_page_content",
    uniqueConstraints = @UniqueConstraint(columnNames = "pageId")
)
public class NotionPageContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String pageId;

    @Column(columnDefinition = "TEXT")
    private String content;

    private Instant syncedAt;

    protected NotionPageContent() {}

    public NotionPageContent(String pageId, String content) {
        this.pageId = pageId;
        this.content = content;
        this.syncedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getPageId() {
        return pageId;
    }

    public String getContent() {
        return content;
    }

    public Instant getSyncedAt() {
        return syncedAt;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setSyncedAt(Instant syncedAt) {
        this.syncedAt = syncedAt;
    }
}

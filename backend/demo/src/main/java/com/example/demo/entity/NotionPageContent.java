package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notion_page_content")
public class NotionPageContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String pageId;

    @Column(name = "app_user_id")
    private Long appUserId;

    @Column(columnDefinition = "TEXT")
    private String content;

    private Instant syncedAt;

    protected NotionPageContent() {}

    public NotionPageContent(String pageId, Long appUserId, String content) {
        this.pageId = pageId;
        this.appUserId = appUserId;
        this.content = content;
        this.syncedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getPageId() {
        return pageId;
    }

    public Long getAppUserId() {
        return appUserId;
    }

    public void setAppUserId(Long appUserId) {
        this.appUserId = appUserId;
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

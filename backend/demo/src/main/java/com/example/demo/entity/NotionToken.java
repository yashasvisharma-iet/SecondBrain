package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "notion_token")
public class NotionToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String accessToken;

    @Column(name = "app_user_id")
    private Long appUserId;

    private String workspaceId;

    private String botId;

    public NotionToken() {
    }

    public NotionToken(Long id, String accessToken, Long appUserId, String workspaceId, String botId) {
        this.id = id;
        this.accessToken = accessToken;
        this.appUserId = appUserId;
        this.workspaceId = workspaceId;
        this.botId = botId;
    }
    public NotionToken(String accessToken, Long appUserId, String workspaceId, String botId) {
        this.accessToken = accessToken;
        this.appUserId = appUserId;
        this.workspaceId = workspaceId;
        this.botId = botId;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public Long getAppUserId() {
        return appUserId;
    }

    public void setAppUserId(Long appUserId) {
        this.appUserId = appUserId;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getBotId() {
        return botId;
    }

    public void setBotId(String botId) {
        this.botId = botId;
    }
    
}

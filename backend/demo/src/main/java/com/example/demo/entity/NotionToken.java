package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(
  uniqueConstraints = @UniqueConstraint(columnNames = "workspaceId")
)
public class NotionToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String accessToken;

    private String workspaceId;

    private String botId;

    protected NotionToken() {
    }

    public NotionToken(Long id, String accessToken, String workspaceId, String botId) {
        this.id = id;
        this.accessToken = accessToken;
        this.workspaceId = workspaceId;
        this.botId = botId;
    }
    public NotionToken(String accessToken, String workspaceId, String botId) {
        this.accessToken = accessToken;
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

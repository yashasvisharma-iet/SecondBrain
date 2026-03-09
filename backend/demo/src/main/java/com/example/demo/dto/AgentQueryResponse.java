package com.example.demo.dto;

import java.util.List;

public record AgentQueryResponse(String answer, List<AgentCitationDto> citations) {
    public record AgentCitationDto(String pageId, Integer chunkIndex, String snippet, String source, String syncedAt) {
    }
}

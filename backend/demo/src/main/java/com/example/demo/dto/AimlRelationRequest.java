package com.example.demo.dto;

import java.util.List;

public record AimlRelationRequest(List<AimlChunkDto> chunks, double threshold) {
}

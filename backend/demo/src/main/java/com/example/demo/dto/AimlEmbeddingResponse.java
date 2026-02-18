package com.example.demo.dto;

import java.util.List;

public record AimlEmbeddingResponse(List<List<Double>> embeddings) {
}


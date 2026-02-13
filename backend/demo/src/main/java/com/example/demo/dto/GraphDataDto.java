package com.example.demo.dto;

import java.util.List;

public record GraphDataDto(List<GraphNodeDto> nodes, List<GraphEdgeDto> edges) {
}

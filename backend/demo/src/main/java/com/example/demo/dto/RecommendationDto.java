package com.example.demo.dto;

public record RecommendationDto(
  String id,
  String label,
  String type, // "note"|"chunk"|"topic"
  double score,
  String reason, // e.g. "2 hops via Topic X; also similar semantic embedding"
  String snippet // optional short text excerpt
){}

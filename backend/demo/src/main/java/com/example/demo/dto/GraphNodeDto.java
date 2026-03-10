package com.example.demo.dto;

public record GraphNodeDto(String id, String label, String type, String genre) {
    public GraphNodeDto(String id, String label, String type) {
        this(id, label, type, null);
    }
}

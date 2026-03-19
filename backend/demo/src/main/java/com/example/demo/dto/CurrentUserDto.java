package com.example.demo.dto;

public record CurrentUserDto(Long id,
                             String email,
                             String name,
                             String avatarUrl,
                             boolean googleConnected,
                             boolean notionConnected) {
}

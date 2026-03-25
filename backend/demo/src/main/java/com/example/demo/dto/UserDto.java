package com.example.demo.dto;

public record UserDto(Long id,
                             String email,
                             String name,
                             String avatarUrl,
                             boolean googleConnected,
                             boolean notionConnected) {
}

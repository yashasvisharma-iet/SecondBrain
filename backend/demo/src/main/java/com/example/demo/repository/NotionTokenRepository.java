package com.example.demo.repository;

import com.example.demo.entity.NotionToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotionTokenRepository extends JpaRepository<NotionToken, Long> {
    List<NotionToken> findAllByWorkspaceIdAndAppUserId(String workspaceId, Long appUserId);

    Optional<NotionToken> findFirstByWorkspaceIdAndAppUserIdOrderByIdDesc(String workspaceId, Long appUserId);

    boolean existsByAppUserId(Long appUserId);
}

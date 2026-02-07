package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.NotionToken;

public interface NotionTokenRepository extends JpaRepository<NotionToken, Long> {
	// return all tokens for a workspace (used for cleanup when duplicates exist)
	List<NotionToken> findAllByWorkspaceId(String workspaceId);

	// convenience to fetch a single (latest) token if needed
	Optional<NotionToken> findFirstByWorkspaceIdOrderByIdDesc(String workspaceId);
}

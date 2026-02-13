package com.example.demo.repository;

import com.example.demo.entity.NotionPageContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotionPageContentRepository
        extends JpaRepository<NotionPageContent, Long> {

    Optional<NotionPageContent> findByPageId(String pageId);

    List<NotionPageContent> findAllByOrderBySyncedAtDesc();
}

package com.example.demo.repository;

import com.example.demo.entity.NotionPageContent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:notion-page-repo;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class NotionPageContentRepositoryTest {

    @Autowired
    private NotionPageContentRepository repository;

    @Test
    void findsPageByPageIdAndAppUserId() {
        NotionPageContent saved = repository.save(new NotionPageContent("page-1", 7L, "Hello"));

        Optional<NotionPageContent> found = repository.findByPageIdAndAppUserId("page-1", 7L);

        assertThat(found).isPresent();
        assertThat(found.orElseThrow().getId()).isEqualTo(saved.getId());
    }

    @Test
    void returnsPagesOrderedByNewestSyncedAtFirst() {
        NotionPageContent older = new NotionPageContent("page-old", 7L, "Older");
        older.setSyncedAt(Instant.parse("2024-01-01T00:00:00Z"));
        NotionPageContent newer = new NotionPageContent("page-new", 7L, "Newer");
        newer.setSyncedAt(Instant.parse("2024-02-01T00:00:00Z"));
        repository.saveAll(List.of(older, newer));

        List<NotionPageContent> results = repository.findAllByAppUserIdOrderBySyncedAtDesc(7L);

        assertThat(results).extracting(NotionPageContent::getPageId).containsExactly("page-new", "page-old");
    }
}

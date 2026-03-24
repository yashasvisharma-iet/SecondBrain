package com.example.demo.repository;

import com.example.demo.entity.NotionToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:notion-token-repo;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class NotionTokenRepositoryTest {

    @Autowired
    private NotionTokenRepository repository;

    @Test
    void findsLatestTokenForWorkspaceAndUser() {
        repository.save(new NotionToken("token-1", 3L, "workspace-1", "bot-1"));
        NotionToken newest = repository.save(new NotionToken("token-2", 3L, "workspace-1", "bot-2"));

        NotionToken found = repository.findFirstByWorkspaceIdAndAppUserIdOrderByIdDesc("workspace-1", 3L).orElseThrow();

        assertThat(found.getId()).isEqualTo(newest.getId());
        assertThat(found.getAccessToken()).isEqualTo("token-2");
    }

    @Test
    void findsLatestTokenForWorkspaceAcrossUsers() {
        repository.save(new NotionToken("token-1", 3L, "workspace-1", "bot-1"));
        NotionToken newest = repository.save(new NotionToken("token-2", 5L, "workspace-1", "bot-2"));

        NotionToken found = repository.findFirstByWorkspaceIdOrderByIdDesc("workspace-1").orElseThrow();

        assertThat(found.getId()).isEqualTo(newest.getId());
        assertThat(found.getAppUserId()).isEqualTo(5L);
        assertThat(found.getAccessToken()).isEqualTo("token-2");
    }

    @Test
    void reportsWhetherUserHasAnyConnectedNotionToken() {
        repository.saveAll(List.of(
                new NotionToken("token-1", 10L, "workspace-1", "bot-1"),
                new NotionToken("token-2", 10L, "workspace-2", "bot-2")
        ));

        assertThat(repository.existsByAppUserId(10L)).isTrue();
        assertThat(repository.existsByAppUserId(11L)).isFalse();
    }
}

package com.example.demo.repository;

import com.example.demo.entity.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:appuser-repo;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AppUserRepositoryTest {

    @Autowired
    private AppUserRepository repository;

    @Test
    void findsUserByAuthProviderAndProviderUserId() {
        AppUser saved = repository.save(new AppUser("alice@example.com", "Alice", "avatar", "google", "sub-123"));

        Optional<AppUser> found = repository.findByAuthProviderAndProviderUserId("google", "sub-123");

        assertThat(found).isPresent();
        assertThat(found.orElseThrow().getId()).isEqualTo(saved.getId());
    }

    @Test
    void findsUserByEmail() {
        repository.save(new AppUser("bob@example.com", "Bob", null, "google", "sub-456"));

        Optional<AppUser> found = repository.findByEmail("bob@example.com");

        assertThat(found).isPresent();
        assertThat(found.orElseThrow().getName()).isEqualTo("Bob");
    }
}

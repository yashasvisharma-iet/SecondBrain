package com.example.demo.repository;

import com.example.demo.entity.TextChunk;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:text-chunk-repo;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class TextChunkRepositoryTest {

    @Autowired
    private TextChunkRepository repository;

    @Test
    void searchesChunkContentCaseInsensitivelyForSpecificUser() {
        TextChunk matching = new TextChunk(null, 1L, 0, "Transformers are excellent for NLP.");
        matching.setAppUserId(9L);
        TextChunk otherUser = new TextChunk(null, 2L, 0, "Transformers are also here.");
        otherUser.setAppUserId(10L);
        repository.saveAll(List.of(matching, otherUser));

        List<TextChunk> results = repository.searchByContent(9L, "transformers", PageRequest.of(0, 10));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getAppUserId()).isEqualTo(9L);
    }

    @Test
    void deletesChunksByRawNoteId() {
        TextChunk keep = new TextChunk(null, 5L, 0, "Keep me");
        keep.setAppUserId(9L);
        TextChunk deleteOne = new TextChunk(null, 6L, 0, "Delete me");
        deleteOne.setAppUserId(9L);
        TextChunk deleteTwo = new TextChunk(null, 6L, 1, "Delete me too");
        deleteTwo.setAppUserId(9L);
        repository.saveAll(List.of(keep, deleteOne, deleteTwo));

        repository.deleteAllByRawNoteId(6L);

        assertThat(repository.findAll()).extracting(TextChunk::getRawNoteId).containsExactly(5L);
    }
}

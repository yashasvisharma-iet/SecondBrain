package com.example.demo.service;

import com.example.demo.entity.AppUser;
import com.example.demo.entity.NotionPageContent;
import com.example.demo.repository.NotionPageContentRepository;
import com.example.demo.repository.NotionTokenRepository;
import com.example.demo.service.chunkingAndEmbedding.ChunkingService;
import com.example.demo.service.ingestion.NotionIngestionService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotionIngestionServiceTest {

    @Mock
    private NotionTokenRepository tokenRepository;
    @Mock
    private NotionPageContentRepository contentRepository;
    @Mock
    private ChunkingService chunkingService;

    @InjectMocks
    private NotionIngestionService service;

    @Test
    void ingestRawContentCreatesPageAndChunksIt() {
        AppUser user = new AppUser("alice@example.com", "Alice", null, "google", "sub");
        setId(user, 8L);
        NotionPageContent saved = new NotionPageContent("page-1", 8L, "Hello world");
        setId(saved, 100L);
        when(contentRepository.findByPageIdAndAppUserId("page-1", 8L)).thenReturn(Optional.empty());
        when(contentRepository.findByPageId("page-1")).thenReturn(Optional.empty());
        when(contentRepository.save(any(NotionPageContent.class))).thenReturn(saved);

        service.ingestRawContent(user, "page-1", "Hello world");

        verify(contentRepository).save(any(NotionPageContent.class));
        verify(chunkingService).chunkNote(100L);
    }

    @Test
    void ingestRawContentRetriesAfterUniqueConstraintViolation() {
        AppUser user = new AppUser("alice@example.com", "Alice", null, "google", "sub");
        setId(user, 8L);
        NotionPageContent existing = new NotionPageContent("page-1", 8L, "Old");
        setId(existing, 101L);
        when(contentRepository.findByPageIdAndAppUserId("page-1", 8L)).thenReturn(Optional.empty(), Optional.of(existing));
        when(contentRepository.findByPageId("page-1")).thenReturn(Optional.empty());
        when(contentRepository.save(any(NotionPageContent.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"))
                .thenReturn(existing);

        service.ingestRawContent(user, "page-1", "Updated");

        verify(contentRepository, times(2)).save(any(NotionPageContent.class));
        verify(chunkingService).chunkNote(101L);
    }

    @Test
    void ingestPageFailsWhenNoTokenExistsForWorkspace() {
        AppUser user = new AppUser("alice@example.com", "Alice", null, "google", "sub");
        setId(user, 8L);
        when(tokenRepository.findFirstByWorkspaceIdAndAppUserIdOrderByIdDesc("workspace-1", 8L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ingestPage(user, "workspace-1", "page-1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No Notion token found");
    }

    @Test
    void ingestRawContentSwallowsChunkingFailuresAfterSaving() {
        AppUser user = new AppUser("alice@example.com", "Alice", null, "google", "sub");
        setId(user, 8L);
        NotionPageContent saved = new NotionPageContent("page-2", 8L, "Hello world");
        setId(saved, 102L);
        when(contentRepository.findByPageIdAndAppUserId("page-2", 8L)).thenReturn(Optional.empty());
        when(contentRepository.findByPageId("page-2")).thenReturn(Optional.empty());
        when(contentRepository.save(any(NotionPageContent.class))).thenReturn(saved);
        doThrow(new IllegalStateException("chunk failed")).when(chunkingService).chunkNote(102L);

        service.ingestRawContent(user, "page-2", "Hello world");

        verify(contentRepository).save(any(NotionPageContent.class));
        verify(chunkingService).chunkNote(102L);
    }

    @Test
    void ingestRawContentReusesPageWhenItAlreadyExistsForAnotherUser() {
        AppUser user = new AppUser("alice@example.com", "Alice", null, "google", "sub");
        setId(user, 8L);
        NotionPageContent existingForDifferentUser = new NotionPageContent("page-3", 9L, "Old");
        setId(existingForDifferentUser, 103L);

        when(contentRepository.findByPageIdAndAppUserId("page-3", 8L)).thenReturn(Optional.empty());
        when(contentRepository.findByPageId("page-3")).thenReturn(Optional.of(existingForDifferentUser));
        when(contentRepository.save(existingForDifferentUser)).thenReturn(existingForDifferentUser);

        service.ingestRawContent(user, "page-3", "Updated");

        verify(contentRepository).save(existingForDifferentUser);
        verify(chunkingService).chunkNote(103L);
    }

    private void setId(Object target, Long id) {
        try {
            var field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}

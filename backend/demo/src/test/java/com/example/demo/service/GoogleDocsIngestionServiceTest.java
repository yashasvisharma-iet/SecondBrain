package com.example.demo.service;

import com.example.demo.entity.NotionPageContent;
import com.example.demo.repository.NotionPageContentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleDocsIngestionServiceTest {

    @Mock
    private NotionPageContentRepository contentRepository;
    @Mock
    private ChunkingService chunkingService;

    @InjectMocks
    private GoogleDocsIngestionService service;

    @Test
    void ingestRawContentCreatesGoogleDocPageAndChunksIt() {
        NotionPageContent saved = new NotionPageContent("gdoc:doc-1", 8L, "Hello world");
        setId(saved, 100L);
        when(contentRepository.findByPageIdAndAppUserId("gdoc:doc-1", 8L)).thenReturn(Optional.empty());
        when(contentRepository.save(any(NotionPageContent.class))).thenReturn(saved);

        service.ingestRawContent(8L, "doc-1", "Hello world");

        verify(contentRepository).save(any(NotionPageContent.class));
        verify(chunkingService).chunkNote(100L);
    }

    @Test
    void ingestRawContentRetriesAfterUniqueConstraintViolation() {
        NotionPageContent existing = new NotionPageContent("gdoc:doc-1", 8L, "Old");
        setId(existing, 101L);
        when(contentRepository.findByPageIdAndAppUserId("gdoc:doc-1", 8L)).thenReturn(Optional.empty(), Optional.of(existing));
        when(contentRepository.save(any(NotionPageContent.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"))
                .thenReturn(existing);

        service.ingestRawContent(8L, "doc-1", "Updated");

        verify(contentRepository, times(2)).save(any(NotionPageContent.class));
        verify(chunkingService).chunkNote(101L);
    }

    @Test
    void ingestRawContentSwallowsChunkingFailuresAfterSaving() {
        NotionPageContent saved = new NotionPageContent("gdoc:doc-2", 8L, "Hello world");
        setId(saved, 102L);
        when(contentRepository.findByPageIdAndAppUserId("gdoc:doc-2", 8L)).thenReturn(Optional.empty());
        when(contentRepository.save(any(NotionPageContent.class))).thenReturn(saved);
        doThrow(new IllegalStateException("chunk failed")).when(chunkingService).chunkNote(102L);

        service.ingestRawContent(8L, "doc-2", "Hello world");

        verify(contentRepository).save(any(NotionPageContent.class));
        verify(chunkingService).chunkNote(102L);
    }

    private void setId(NotionPageContent page, Long id) {
        try {
            var field = NotionPageContent.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(page, id);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}

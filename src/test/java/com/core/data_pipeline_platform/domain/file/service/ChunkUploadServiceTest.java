package com.core.data_pipeline_platform.domain.file.service;

import com.core.data_pipeline_platform.domain.file.dto.ChunkUploadStartRequest;
import com.core.data_pipeline_platform.domain.file.dto.ChunkUploadStartResponse;
import com.core.data_pipeline_platform.domain.file.repository.ChunkUploadSessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ChunkUploadServiceTest {

    @Mock
    private ChunkUploadSessionRepository chunkUploadSessionRepository;

    @InjectMocks
    private ChunkUploadService chunkUploadService;

    @Test
    @DisplayName("청크 업로드 시작 - 성공")
    void startUpload_Success() {
        // Given
        ChunkUploadStartRequest request = new ChunkUploadStartRequest(
                "test.csv",  1000L, 10
        );

        // When
        ChunkUploadStartResponse response = chunkUploadService.startUpload(request);

        // Then
        assertThat(response.sessionId()).isNotNull();
    }

    @Test
    @DisplayName("잘못된 파일 이름")
    void invalid_fileName() {
        // Given
        ChunkUploadStartRequest request = new ChunkUploadStartRequest(
                "invalid.txt", 1000L, 10
        );

        // When & Then
        assertThrows(ResponseStatusException.class, () -> chunkUploadService.startUpload(request));
    }


}
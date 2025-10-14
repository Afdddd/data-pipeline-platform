package com.core.data_pipeline_platform.domain.file.service;

import com.core.data_pipeline_platform.domain.file.dto.*;
import com.core.data_pipeline_platform.domain.file.entity.ChunkUploadSession;
import com.core.data_pipeline_platform.domain.file.entity.FileEntity;
import com.core.data_pipeline_platform.domain.file.enums.ChunkUploadStatus;
import com.core.data_pipeline_platform.domain.file.repository.ChunkUploadSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ChunkUploadServiceTest {

    @Mock
    private ChunkUploadSessionRepository chunkUploadSessionRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private ChunkUploadService chunkUploadService;

    @TempDir
    Path chunkUploadDir;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fileStorageService, "chunkUploadDir", chunkUploadDir.toString());
    }

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

    @Test
    @DisplayName("청크 업로드 - 성공")
    void chunkUpload_Success() {
        // Given
        ChunkUploadRequest request = new ChunkUploadRequest("sessionId", 1, new byte[10], 100);

        ChunkUploadSession session = ChunkUploadSession.builder()
                .id(1L)
                .sessionId("sessionId")
                .totalChunks(10)
                .completedChunks(0)
                .status(ChunkUploadStatus.PENDING)
                .chunkInfo("{}")
                .build();

        given(chunkUploadSessionRepository.findBySessionId(request.sessionId()))
                .willReturn(Optional.of(session));

        // When
        ChunkUploadResponse response = chunkUploadService.upload(request);

        // Then
        assertThat(response.progress()).isEqualTo(10); // 1/10 * 100 = 10%
        
        // 메서드 호출 검증
        then(chunkUploadSessionRepository).should().findBySessionId("sessionId");
        then(fileStorageService).should().storeChunk(request);
    }

    @Test
    @DisplayName("첫 청크 성공 시 상태가 IN_PROGRESS로 전이")
    void upload_FirstChunkSuccess_StatusChangesToInProgress() {
        // Given
        ChunkUploadRequest request = new ChunkUploadRequest("sessionId", 0, new byte[10], 100);

        ChunkUploadSession session = ChunkUploadSession.builder()
                .id(1L)
                .sessionId("sessionId")
                .totalChunks(5)
                .completedChunks(0)
                .status(ChunkUploadStatus.PENDING)
                .chunkInfo("{}")
                .build();

        given(chunkUploadSessionRepository.findBySessionId(request.sessionId()))
                .willReturn(Optional.of(session));

        // When
        ChunkUploadResponse response = chunkUploadService.upload(request);

        // Then
        assertThat(response.progress()).isEqualTo(20); // 1/5 * 100 = 20%
        assertThat(session.getStatus()).isEqualTo(ChunkUploadStatus.IN_PROGRESS);
        
        then(fileStorageService).should().storeChunk(request);
    }

    @Test
    @DisplayName("중복 전송 시 storeChunk 미호출, 진행률 동일")
    void upload_DuplicateChunk_NoStoreCall_SameProgress() {
        // Given
        ChunkUploadRequest request = new ChunkUploadRequest("sessionId", 0, new byte[10], 100);

        ChunkUploadSession session = ChunkUploadSession.builder()
                .id(1L)
                .sessionId("sessionId")
                .totalChunks(5)
                .completedChunks(1) // 이미 1개 완료
                .status(ChunkUploadStatus.IN_PROGRESS)
                .chunkInfo("{\"0\":\"COMPLETED\"}") // 인덱스 0 이미 완료
                .build();

        given(chunkUploadSessionRepository.findBySessionId(request.sessionId()))
                .willReturn(Optional.of(session));

        // When
        ChunkUploadResponse response = chunkUploadService.upload(request);

        // Then
        assertThat(response.progress()).isEqualTo(20); // 1/5 * 100 = 20% (변화 없음)
        
        // storeChunk 호출되지 않아야 함
        then(fileStorageService).should(never()).storeChunk(any());
    }

    @Test
    @DisplayName("범위 밖 청크 인덱스 - 400 예외")
    void upload_InvalidChunkIndex_ThrowsBadRequest() {
        // Given
        ChunkUploadRequest request = new ChunkUploadRequest("sessionId", 5, new byte[10], 100); // 범위 밖

        ChunkUploadSession session = ChunkUploadSession.builder()
                .id(1L)
                .sessionId("sessionId")
                .totalChunks(5) // 0-4만 유효
                .completedChunks(0)
                .status(ChunkUploadStatus.PENDING)
                .chunkInfo("{}")
                .build();

        given(chunkUploadSessionRepository.findBySessionId(request.sessionId()))
                .willReturn(Optional.of(session));

        // When & Then
        assertThatThrownBy(() -> chunkUploadService.upload(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException responseEx = (ResponseStatusException) ex;
                    assertThat(responseEx.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(responseEx.getReason()).isEqualTo("유효하지 않은 청크 인덱스 입니다.");
                });
        
        then(fileStorageService).should(never()).storeChunk(any());
    }

    @Test
    @DisplayName("완료된 세션에 업로드 시도 - 400 예외")
    void upload_CompletedSession_ThrowsBadRequest() {
        // Given
        ChunkUploadRequest request = new ChunkUploadRequest("sessionId", 0, new byte[10], 100);

        ChunkUploadSession session = ChunkUploadSession.builder()
                .id(1L)
                .sessionId("sessionId")
                .totalChunks(5)
                .completedChunks(5)
                .status(ChunkUploadStatus.COMPLETED)
                .chunkInfo("{}")
                .build();

        given(chunkUploadSessionRepository.findBySessionId(request.sessionId()))
                .willReturn(Optional.of(session));

        // When & Then
        assertThatThrownBy(() -> chunkUploadService.upload(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException responseEx = (ResponseStatusException) ex;
                    assertThat(responseEx.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(responseEx.getReason()).isEqualTo("이미 완료된 세션입니다.");
                });
        
        then(fileStorageService).should(never()).storeChunk(any());
    }

    @Test
    @DisplayName("완료 - 실패 청크 존재 시 success=false, status=FAILED")
    void completeUpload_HasFailedChunks_ReturnsFailure() {
        // Given
        String sessionId = "sessionId";
        
        ChunkUploadSession session = ChunkUploadSession.builder()
                .id(1L)
                .sessionId(sessionId)
                .totalChunks(5)
                .completedChunks(3)
                .status(ChunkUploadStatus.IN_PROGRESS)
                .chunkInfo("{\"0\":\"COMPLETED\",\"1\":\"COMPLETED\",\"2\":\"COMPLETED\",\"3\":\"FAILED\"}") // 3번 실패
                .build();

        given(chunkUploadSessionRepository.findBySessionId(sessionId))
                .willReturn(Optional.of(session));

        // When
        ChunkUploadCompleteResponse response = chunkUploadService.completeUpload(sessionId);

        // Then
        assertThat(response.success()).isFalse();
        assertThat(response.message()).isEqualTo("일부 청크 업로드 실패");
        assertThat(response.fileId()).isNull();
        assertThat(session.getStatus()).isEqualTo(ChunkUploadStatus.FAILED);
        
        // mergeChunks 호출되지 않아야 함
        then(fileStorageService).should(never()).mergeChunks(any());
    }

    @Test
    @DisplayName("완료 - 모든 청크 성공 시 success=true, status=COMPLETED")
    void completeUpload_AllChunksSuccess_ReturnsSuccess() {
        // Given
        String sessionId = "sessionId";
        
        ChunkUploadSession session = ChunkUploadSession.builder()
                .id(1L)
                .sessionId(sessionId)
                .totalChunks(3)
                .completedChunks(3)
                .status(ChunkUploadStatus.IN_PROGRESS)
                .chunkInfo("{\"0\":\"COMPLETED\",\"1\":\"COMPLETED\",\"2\":\"COMPLETED\"}")
                .build();

        FileEntity mergedFile = FileEntity.builder()
                .id(123L)
                .build();

        given(chunkUploadSessionRepository.findBySessionId(sessionId))
                .willReturn(Optional.of(session));
        given(fileStorageService.mergeChunks(session))
                .willReturn(mergedFile);

        // When
        ChunkUploadCompleteResponse response = chunkUploadService.completeUpload(sessionId);

        // Then
        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("업로드 완료");
        assertThat(response.fileId()).isEqualTo("123");
        assertThat(session.getStatus()).isEqualTo(ChunkUploadStatus.COMPLETED);
        
        then(fileStorageService).should().mergeChunks(session);
    }

    @Test
    @DisplayName("완료 - 세션 없음 - 400 예외")
    void completeUpload_SessionNotFound_ThrowsBadRequest() {
        // Given
        String sessionId = "nonExistentSession";
        
        given(chunkUploadSessionRepository.findBySessionId(sessionId))
                .willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> chunkUploadService.completeUpload(sessionId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException responseEx = (ResponseStatusException) ex;
                    assertThat(responseEx.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(responseEx.getReason()).isEqualTo("서버에 세션이 없습니다.");
                });
    }

    @Test
    @DisplayName("취소 - 성공")
    void cancelUpload_Success() {
        // Given
        String sessionId = "sessionId";
        
        ChunkUploadSession session = ChunkUploadSession.builder()
                .id(1L)
                .sessionId(sessionId)
                .totalChunks(5)
                .completedChunks(2)
                .status(ChunkUploadStatus.IN_PROGRESS)
                .chunkInfo("{\"0\":\"COMPLETED\",\"1\":\"COMPLETED\"}")
                .build();

        given(chunkUploadSessionRepository.findBySessionId(sessionId))
                .willReturn(Optional.of(session));

        // When
        ChunkUploadCancelResponse response = chunkUploadService.cancelUpload(sessionId);

        // Then
        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("업로드가 취소되었습니다.");
        assertThat(session.getStatus()).isEqualTo(ChunkUploadStatus.CANCELLED);
        
        then(fileStorageService).should().cleanupChunkFiles(session);
    }

    @Test
    @DisplayName("취소 - 이미 취소된 세션")
    void cancelUpload_AlreadyCancelled_ReturnsSuccess() {
        // Given
        String sessionId = "sessionId";
        
        ChunkUploadSession session = ChunkUploadSession.builder()
                .id(1L)
                .sessionId(sessionId)
                .totalChunks(5)
                .completedChunks(2)
                .status(ChunkUploadStatus.CANCELLED)
                .chunkInfo("{\"0\":\"COMPLETED\",\"1\":\"COMPLETED\"}")
                .build();

        given(chunkUploadSessionRepository.findBySessionId(sessionId))
                .willReturn(Optional.of(session));

        // When
        ChunkUploadCancelResponse response = chunkUploadService.cancelUpload(sessionId);

        // Then
        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("이미 취소된 세션입니다.");
        
        // cleanupChunkFiles 호출되지 않아야 함
        then(fileStorageService).should(never()).cleanupChunkFiles(any());
    }

    @Test
    @DisplayName("취소 - 완료된 세션 - 400 예외")
    void cancelUpload_CompletedSession_ThrowsBadRequest() {
        // Given
        String sessionId = "sessionId";
        
        ChunkUploadSession session = ChunkUploadSession.builder()
                .id(1L)
                .sessionId(sessionId)
                .totalChunks(5)
                .completedChunks(5)
                .status(ChunkUploadStatus.COMPLETED)
                .chunkInfo("{}")
                .build();

        given(chunkUploadSessionRepository.findBySessionId(sessionId))
                .willReturn(Optional.of(session));

        // When & Then
        assertThatThrownBy(() -> chunkUploadService.cancelUpload(sessionId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException responseEx = (ResponseStatusException) ex;
                    assertThat(responseEx.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(responseEx.getReason()).isEqualTo("이미 완료된 세션입니다.");
                });
        
        then(fileStorageService).should(never()).cleanupChunkFiles(any());
    }

    @Test
    @DisplayName("취소 - 세션 없음 - 400 예외")
    void cancelUpload_SessionNotFound_ThrowsBadRequest() {
        // Given
        String sessionId = "nonExistentSession";
        
        given(chunkUploadSessionRepository.findBySessionId(sessionId))
                .willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> chunkUploadService.cancelUpload(sessionId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException responseEx = (ResponseStatusException) ex;
                    assertThat(responseEx.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(responseEx.getReason()).isEqualTo("서버에 세션이 없습니다.");
                });
    }

    @Test
    @DisplayName("업로드 - 취소된 세션에 업로드 시도 - 400 예외")
    void upload_CancelledSession_ThrowsBadRequest() {
        // Given
        ChunkUploadRequest request = new ChunkUploadRequest("sessionId", 0, new byte[10], 100);

        ChunkUploadSession session = ChunkUploadSession.builder()
                .id(1L)
                .sessionId("sessionId")
                .totalChunks(5)
                .completedChunks(0)
                .status(ChunkUploadStatus.CANCELLED)
                .chunkInfo("{}")
                .build();

        given(chunkUploadSessionRepository.findBySessionId(request.sessionId()))
                .willReturn(Optional.of(session));

        // When & Then
        assertThatThrownBy(() -> chunkUploadService.upload(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException responseEx = (ResponseStatusException) ex;
                    assertThat(responseEx.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(responseEx.getReason()).isEqualTo("취소된 세션입니다.");
                });
        
        then(fileStorageService).should(never()).storeChunk(any());
    }

}
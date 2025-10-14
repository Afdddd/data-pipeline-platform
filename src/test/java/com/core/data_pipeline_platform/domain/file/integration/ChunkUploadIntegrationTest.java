package com.core.data_pipeline_platform.domain.file.integration;

import com.core.data_pipeline_platform.domain.file.dto.*;
import com.core.data_pipeline_platform.domain.file.entity.ChunkUploadSession;
import com.core.data_pipeline_platform.domain.file.entity.FileEntity;
import com.core.data_pipeline_platform.domain.file.enums.ChunkUploadStatus;
import com.core.data_pipeline_platform.domain.file.enums.FileType;
import com.core.data_pipeline_platform.domain.file.repository.ChunkUploadSessionRepository;
import com.core.data_pipeline_platform.domain.file.repository.FileRepository;
import com.core.data_pipeline_platform.domain.file.service.ChunkUploadService;
import com.core.data_pipeline_platform.domain.file.service.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("청크 업로드 통합 테스트")
class ChunkUploadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ChunkUploadService chunkUploadService;

    @Autowired
    private ChunkUploadSessionRepository sessionRepository;

    @Autowired
    private FileRepository fileRepository;

    @MockitoBean
    private FileStorageService fileStorageService;

    private Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        // 임시 디렉토리 생성
        tempDir = Files.createTempDirectory("chunk-upload-test");
        
        // 테스트 데이터 정리
        sessionRepository.deleteAll();
        fileRepository.deleteAll();

        // FileStorageService Mock 설정
        setupFileStorageServiceMock();
    }

    private void setupFileStorageServiceMock() {
        // storeChunk 호출 시 아무것도 하지 않음 (성공으로 처리)
        doNothing().when(fileStorageService).storeChunk(any());

        // mergeChunks 호출 시 가짜 FileEntity 반환
        FileEntity mockFileEntity = FileEntity.builder()
                .id(1L)
                .originName("test-file")
                .fileType(FileType.CSV)
                .directoryName("test-dir")
                .storedName("test-stored")
                .build();

        given(fileStorageService.mergeChunks(any(ChunkUploadSession.class)))
                .willReturn(mockFileEntity);
    }

    @Test
    @DisplayName("전체 청크 업로드 플로우 - 성공")
    void completeChunkUploadFlow_Success() throws Exception {
        // Given: 3개 청크로 나눈 파일 업로드 시나리오
        String fileName = "test-data.csv";
        long totalSize = 300L;
        int totalChunks = 3;

        // 1. 세션 시작
        ChunkUploadStartRequest startRequest = new ChunkUploadStartRequest(
                fileName, totalSize, totalChunks
        );

        String startRequestJson = objectMapper.writeValueAsString(startRequest);

        String sessionIdResponse = mockMvc.perform(post("/api/files/chunk/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(startRequestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String sessionId = objectMapper.readTree(sessionIdResponse).get("sessionId").asText();

        // 세션 상태 확인
        Optional<ChunkUploadSession> sessionOpt = sessionRepository.findBySessionId(sessionId);
        assertThat(sessionOpt).isPresent();
        assertThat(sessionOpt.get().getStatus()).isEqualTo(ChunkUploadStatus.PENDING);

        // 2. 청크들 순차 업로드
        for (int i = 0; i < totalChunks; i++) {
            byte[] chunkData = ("chunk-data-" + i).getBytes();
            
            ChunkUploadRequest uploadRequest = new ChunkUploadRequest(
                    sessionId, i, chunkData, chunkData.length
            );

            String uploadRequestJson = objectMapper.writeValueAsString(uploadRequest);

            mockMvc.perform(post("/api/files/chunk/upload")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(uploadRequestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.progress").exists());

            // 세션 상태 확인 (첫 청크 후 IN_PROGRESS)
            sessionOpt = sessionRepository.findBySessionId(sessionId);
            assertThat(sessionOpt).isPresent();
            if (i == 0) {
                assertThat(sessionOpt.get().getStatus()).isEqualTo(ChunkUploadStatus.IN_PROGRESS);
            }
        }

        // 3. 완료 처리
        String completeResponse = mockMvc.perform(post("/api/files/chunk/complete/{sessionId}", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("업로드 완료"))
                .andExpect(jsonPath("$.fileId").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 4. 최종 상태 확인
        sessionOpt = sessionRepository.findBySessionId(sessionId);
        assertThat(sessionOpt).isPresent();
        assertThat(sessionOpt.get().getStatus()).isEqualTo(ChunkUploadStatus.COMPLETED);

        // 5. 응답에서 fileId 확인 (Mock에서 반환한 ID)
        String fileId = objectMapper.readTree(completeResponse).get("fileId").asText();
        assertThat(fileId).isEqualTo("1"); // Mock에서 설정한 ID
    }

    @Test
    @DisplayName("중복 청크 전송 - idempotency 검증")
    void duplicateChunkUpload_Idempotency() throws Exception {
        // Given
        String fileName = "test.json";
        long totalSize = 100L;
        int totalChunks = 2;

        // 1. 세션 시작
        ChunkUploadStartRequest startRequest = new ChunkUploadStartRequest(
                fileName, totalSize, totalChunks
        );

        String sessionId = startSessionAndGetId(startRequest);

        // 2. 첫 번째 청크 업로드
        ChunkUploadRequest firstUpload = new ChunkUploadRequest(
                sessionId, 0, "chunk-0".getBytes(), 7
        );

        String firstResponse = mockMvc.perform(post("/api/files/chunk/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstUpload)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        int firstProgress = objectMapper.readTree(firstResponse).get("progress").asInt();

        // 3. 동일 청크 재전송
        String secondResponse = mockMvc.perform(post("/api/files/chunk/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstUpload)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        int secondProgress = objectMapper.readTree(secondResponse).get("progress").asInt();

        // 4. 진행률이 동일해야 함 (idempotent)
        assertThat(secondProgress).isEqualTo(firstProgress);

        // 5. 세션 상태 확인
        Optional<ChunkUploadSession> sessionOpt = sessionRepository.findBySessionId(sessionId);
        assertThat(sessionOpt).isPresent();
        assertThat(sessionOpt.get().getCompletedChunks()).isEqualTo(1); // 1개만 증가
    }

    @Test
    @DisplayName("범위 밖 청크 인덱스 - 400 에러")
    void uploadOutOfRangeChunk_ReturnsBadRequest() throws Exception {
        // Given
        String fileName = "test.csv";
        long totalSize = 100L;
        int totalChunks = 2;

        ChunkUploadStartRequest startRequest = new ChunkUploadStartRequest(
                fileName, totalSize, totalChunks
        );

        String sessionId = startSessionAndGetId(startRequest);

        // 범위 밖 인덱스 (2, totalChunks=2이므로 0,1만 유효)
        ChunkUploadRequest outOfRangeRequest = new ChunkUploadRequest(
                sessionId, 2, "invalid".getBytes(), 7
        );

        // When & Then
        mockMvc.perform(post("/api/files/chunk/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(outOfRangeRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("존재하지 않는 세션으로 업로드 - 400 에러")
    void uploadWithNonExistentSession_ReturnsBadRequest() throws Exception {
        // Given
        String nonExistentSessionId = "non-existent-session";
        ChunkUploadRequest request = new ChunkUploadRequest(
                nonExistentSessionId, 0, "data".getBytes(), 4
        );

        // When & Then
        mockMvc.perform(post("/api/files/chunk/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("지원하지 않는 파일 형식 - 400 에러")
    void startWithUnsupportedFileType_ReturnsBadRequest() throws Exception {
        // Given
        ChunkUploadStartRequest request = new ChunkUploadStartRequest(
                "test.txt", 100L, 2  // .txt는 지원하지 않음
        );

        // When & Then
        mockMvc.perform(post("/api/files/chunk/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("취소 - 성공")
    void cancelUpload_Success() throws Exception {
        // Given
        String fileName = "test.csv";
        long totalSize = 100L;
        int totalChunks = 3;

        ChunkUploadStartRequest startRequest = new ChunkUploadStartRequest(
                fileName, totalSize, totalChunks
        );

        String sessionId = startSessionAndGetId(startRequest);

        // 몇 개 청크 업로드
        for (int i = 0; i < 2; i++) {
            byte[] chunkData = ("chunk-" + i).getBytes();
            
            ChunkUploadRequest uploadRequest = new ChunkUploadRequest(
                    sessionId, i, chunkData, chunkData.length
            );

            mockMvc.perform(post("/api/files/chunk/upload")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(uploadRequest)))
                    .andExpect(status().isOk());
        }

        // 취소 처리
        mockMvc.perform(post("/api/files/chunk/cancel/{sessionId}", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("업로드가 취소되었습니다."));

        // 세션 상태 확인
        Optional<ChunkUploadSession> sessionOpt = sessionRepository.findBySessionId(sessionId);
        assertThat(sessionOpt).isPresent();
        assertThat(sessionOpt.get().getStatus()).isEqualTo(ChunkUploadStatus.CANCELLED);

        // 취소된 세션에 업로드 시도 시 400 에러
        byte[] chunkData = ("chunk-2").getBytes();
        ChunkUploadRequest uploadRequest = new ChunkUploadRequest(
                sessionId, 2, chunkData, chunkData.length
        );

        mockMvc.perform(post("/api/files/chunk/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(uploadRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("취소 - 완료된 세션 - 400 에러")
    void cancelUpload_CompletedSession_ReturnsBadRequest() throws Exception {
        // Given
        String fileName = "test.csv";
        long totalSize = 100L;
        int totalChunks = 2;

        String sessionId = startSessionAndGetId(new ChunkUploadStartRequest(fileName, totalSize, totalChunks));

        // 모든 청크 업로드 후 완료
        for (int i = 0; i < totalChunks; i++) {
            byte[] chunkData = ("chunk-" + i).getBytes();
            
            ChunkUploadRequest uploadRequest = new ChunkUploadRequest(
                    sessionId, i, chunkData, chunkData.length
            );

            mockMvc.perform(post("/api/files/chunk/upload")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(uploadRequest)))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/files/chunk/complete/{sessionId}", sessionId))
                .andExpect(status().isOk());

        // 완료된 세션 취소 시도
        mockMvc.perform(post("/api/files/chunk/cancel/{sessionId}", sessionId))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("취소 - 세션 없음 - 400 에러")
    void cancelUpload_SessionNotFound_ReturnsBadRequest() throws Exception {
        // Given
        String nonExistentSessionId = "non-existent-session";

        // When & Then
        mockMvc.perform(post("/api/files/chunk/cancel/{sessionId}", nonExistentSessionId))
                .andExpect(status().isBadRequest());
    }

    private String startSessionAndGetId(ChunkUploadStartRequest request) throws Exception {
        String response = mockMvc.perform(post("/api/files/chunk/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("sessionId").asText();
    }
}

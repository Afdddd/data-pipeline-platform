package com.core.data_pipeline_platform.domain.file.controller;

import com.core.data_pipeline_platform.domain.file.service.FileUploadService;
import com.core.data_pipeline_platform.domain.file.validator.FileValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileUploadController.class)  // Controller만 테스트
@DisplayName("FileUploadController 테스트")
class FileUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;  // HTTP 요청 시뮬레이션

    @MockitoBean  // Spring Context에 Mock Bean 등록
    private FileUploadService fileUploadService;

    @MockitoBean
    private FileValidator fileValidator;

    private MockMultipartFile validFile;
    private MockMultipartFile invalidFile;

    @BeforeEach
    void setUp() {
        // 유효한 파일
        validFile = new MockMultipartFile(
            "file",                     // 파라미터 이름
            "test.json",                      // 원본 파일명
            MediaType.APPLICATION_JSON_VALUE, // Content Type
            "test content".getBytes()         // 파일 내용
        );

        // 무효한 파일 (빈 파일)
        invalidFile = new MockMultipartFile(
            "file",
            "empty.json",
            MediaType.APPLICATION_JSON_VALUE,
            new byte[0]  // 빈 내용
        );
    }

    @Test
    @DisplayName("정상적인 파일 업로드 - 성공")
    void uploadFile_ValidFile_ReturnsFileId() throws Exception {
        // Given
        Long expectedFileId = 1L;
        
        doNothing().when(fileValidator).validateFile(any());  // 검증 통과
        given(fileUploadService.uploadFile(any())).willReturn(expectedFileId);

        // When & Then
        mockMvc.perform(multipart("/api/files/upload")
                .file(validFile))  // 파일 첨부
                .andDo(print())    // 요청/응답 출력
                .andExpect(status().isOk())  // 200 OK
                .andExpect(content().string("1"));  // 응답 본문 검증

        // 호출 검증
        then(fileValidator).should().validateFile(any());
        then(fileUploadService).should().uploadFile(any());
    }

    @Test
    @DisplayName("파일 검증 실패 - 400 Bad Request")
    void uploadFile_ValidationFails_ReturnsBadRequest() throws Exception {
        // Given
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일이 비어있습니다."))
            .when(fileValidator).validateFile(any());

        // When & Then
        mockMvc.perform(multipart("/api/files/upload")
                .file(invalidFile))
                .andDo(print())
                .andExpect(status().isBadRequest());  // 400 Bad Request

        // Service는 호출되지 않아야 함
        then(fileUploadService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("서비스 레이어 예외 - 400 Bad Request")
    void uploadFile_ServiceThrowsException_ReturnsBadRequest() throws Exception {
        // Given
        doNothing().when(fileValidator).validateFile(any());  // 검증 통과
        given(fileUploadService.uploadFile(any()))
            .willThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 파일 형식입니다."));

        // When & Then
        mockMvc.perform(multipart("/api/files/upload")
                .file(validFile))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // 모든 메서드가 호출되었는지 검증
        then(fileValidator).should().validateFile(any());
        then(fileUploadService).should().uploadFile(any());
    }

    @Test
    @DisplayName("파일 파라미터 누락 - 400 Bad Request")
    void uploadFile_MissingFileParameter_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(multipart("/api/files/upload"))  // 파일 없이 요청
                .andDo(print())
                .andExpect(status().isBadRequest());

        // 아무 메서드도 호출되지 않아야 함
        then(fileValidator).shouldHaveNoInteractions();
        then(fileUploadService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("잘못된 HTTP 메서드 - 405 Method Not Allowed")
    void uploadFile_WrongHttpMethod_ReturnsMethodNotAllowed() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/files/upload"))  // GET 요청 (POST여야 함)
                .andDo(print())
                .andExpect(status().isMethodNotAllowed());  // 405 Method Not Allowed
    }

    @Test
    @DisplayName("여러 파일 업로드 시도 - 첫 번째 파일만 처리")
    void uploadFile_MultipleFiles_ProcessesFirstFile() throws Exception {
        // Given
        MockMultipartFile secondFile = new MockMultipartFile(
            "file", "second.json", MediaType.APPLICATION_JSON_VALUE, "second".getBytes()
        );
        
        Long expectedFileId = 2L;
        doNothing().when(fileValidator).validateFile(any());
        given(fileUploadService.uploadFile(any())).willReturn(expectedFileId);

        // When & Then
        mockMvc.perform(multipart("/api/files/upload")
                .file(validFile)
                .file(secondFile))  // 두 개 파일 전송
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("2"));

        // 한 번만 호출되어야 함 (첫 번째 파일만 처리)
        then(fileValidator).should().validateFile(any());
        then(fileUploadService).should().uploadFile(any());
    }

    @Test
    @DisplayName("파일 크기 초과 - 400 Bad Request")
    void uploadFile_FileSizeExceeded_ReturnsBadRequest() throws Exception {
        // Given
        // 큰 파일 생성
        byte[] largeContent = new byte[1024]; // 작은 크기로 테스트 (실제로는 validator에서 검증)
        MockMultipartFile largeFile = new MockMultipartFile(
            "file", 
            "large.json", 
            "application/json", 
            largeContent
        );
        
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일 크기가 제한을 초과합니다"))
            .when(fileValidator).validateFile(any());

        // When & Then
        mockMvc.perform(multipart("/api/files/upload")
                .file(largeFile))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // Service는 호출되지 않아야 함
        then(fileUploadService).shouldHaveNoInteractions();
    }
}

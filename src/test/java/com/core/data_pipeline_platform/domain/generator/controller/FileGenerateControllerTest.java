package com.core.data_pipeline_platform.domain.generator.controller;

import com.core.data_pipeline_platform.domain.generator.dto.GenerateRequest;
import com.core.data_pipeline_platform.domain.generator.service.BinFileGenerator;
import com.core.data_pipeline_platform.domain.generator.service.CsvFileGenerator;
import com.core.data_pipeline_platform.domain.generator.service.JsonFileGenerator;
import com.core.data_pipeline_platform.domain.generator.service.XmlFileGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileGenerateController.class)
@DisplayName("FileGenerateController 테스트")
class FileGenerateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // 실제 컨트롤러가 의존하는 4개의 Generator를 모두 Mock으로 생성
    @MockitoBean
    private JsonFileGenerator jsonFileGenerator;

    @MockitoBean
    private CsvFileGenerator csvFileGenerator;

    @MockitoBean
    private XmlFileGenerator xmlFileGenerator;

    @MockitoBean
    private BinFileGenerator binFileGenerator;

    private GenerateRequest validRequest;
    private byte[] testContent;

    @BeforeEach
    void setUp() {
        validRequest = new GenerateRequest("test-file", 10);
        testContent = "test content".getBytes();
    }

    @ParameterizedTest
    @ValueSource(strings = {"json", "csv", "xml", "bin"})
    @DisplayName("각 파일 포맷별 성공적인 파일 생성 테스트")
    void generateFile_validRequest_allFormats(String format) throws Exception {
        // Given: 각 포맷에 맞는 Generator Mock 설정
        setupMockForFormat(format);

        String expectedMimeType = getExpectedMimeType(format);
        String requestJson = objectMapper.writeValueAsString(validRequest);

        // When & Then
        mockMvc.perform(post("/api/generate/" + format)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + validRequest.fileName() + "\""))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, expectedMimeType))
                .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, String.valueOf(testContent.length)))
                .andExpect(content().bytes(testContent));
    }

    @Test
    @DisplayName("유효하지 않은 요청 - recordCount가 0인 경우")
    void generateFile_invalidRequest_zeroRecordCount() throws Exception {
        // Given
        GenerateRequest invalidRequest = new GenerateRequest("test.json", 0);
        String requestJson = objectMapper.writeValueAsString(invalidRequest);

        // When & Then
        mockMvc.perform(post("/api/generate/json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("유효하지 않은 요청 - recordCount가 1000을 초과하는 경우")
    void generateFile_invalidRequest_exceedsMaxRecordCount() throws Exception {
        // Given
        GenerateRequest invalidRequest = new GenerateRequest("test.json", 1001);
        String requestJson = objectMapper.writeValueAsString(invalidRequest);

        // When & Then
        mockMvc.perform(post("/api/generate/json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("유효하지 않은 요청 - fileName이 비어있는 경우")
    void generateFile_invalidRequest_blankFileName() throws Exception {
        // Given
        String requestJson = "{\"fileName\":\"\",\"recordCount\":10}";

        // When & Then
        mockMvc.perform(post("/api/generate/json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("잘못된 JSON 형식의 요청")
    void generateFile_malformedJson() throws Exception {
        // Given
        String malformedJson = "{\"fileName\":\"test.json\",\"recordCount\":}"; // 잘못된 JSON

        // When & Then
        mockMvc.perform(post("/api/generate/json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // Helper 메서드들
    private void setupMockForFormat(String format) {
        switch (format) {
            case "json" -> Mockito.when(jsonFileGenerator.generatorFile(any(GenerateRequest.class)))
                    .thenReturn(testContent);
            case "csv" -> Mockito.when(csvFileGenerator.generatorFile(any(GenerateRequest.class)))
                    .thenReturn(testContent);
            case "xml" -> Mockito.when(xmlFileGenerator.generatorFile(any(GenerateRequest.class)))
                    .thenReturn(testContent);
            case "bin" -> Mockito.when(binFileGenerator.generatorFile(any(GenerateRequest.class)))
                    .thenReturn(testContent);
        }
    }

    private String getExpectedMimeType(String format) {
        return switch (format) {
            case "json" -> "application/json";
            case "csv" -> "text/csv";
            case "xml" -> "application/xml";
            case "bin" -> "application/octet-stream";
            default -> throw new IllegalArgumentException("Unexpected format: " + format);
        };
    }
}
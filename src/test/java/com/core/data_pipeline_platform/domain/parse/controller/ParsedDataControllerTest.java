package com.core.data_pipeline_platform.domain.parse.controller;

import com.core.data_pipeline_platform.domain.file.entity.FileEntity;
import com.core.data_pipeline_platform.domain.file.enums.FileType;
import com.core.data_pipeline_platform.domain.parse.entity.ParsedDataEntity;
import com.core.data_pipeline_platform.domain.parse.service.ParsedDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ParsedDataController.class)
@DisplayName("ParsedDataController 테스트")
class ParsedDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ParsedDataService parsedDataService;

    @Autowired
    private ObjectMapper objectMapper;

    private FileEntity mockFile;
    private ParsedDataEntity mockParsedData;
    private List<Map<String, Object>> mockData;

    @BeforeEach
    void setUp() {
        mockFile = FileEntity.builder()
            .id(1L)
            .fileType(FileType.JSON)
            .originName("test.json")
            .build();

        mockParsedData = ParsedDataEntity.builder()
            .id(1L)
            .file(mockFile)
            .data("[{\"name\":\"John\",\"age\":25}]")
            .build();

        mockData = Arrays.asList(
            Map.of("name", "John", "age", 25),
            Map.of("name", "Jane", "age", 30)
        );
    }

    @Test
    @DisplayName("모든 파싱된 데이터 조회 - 성공")
    void getAllParsedData_Success() throws Exception {
        // Given
        List<ParsedDataEntity> entities = Arrays.asList(mockParsedData);
        Page<ParsedDataEntity> page = new PageImpl<>(entities, PageRequest.of(0, 10), 1);

        given(parsedDataService.getAllParsedData(any()))
            .willReturn(page);

        // When & Then
        mockMvc.perform(get("/api/parsed-data")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].id").value(1))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("파일 ID로 파싱된 데이터 조회 - 성공")
    void getParsedDataByFileId_Success() throws Exception {
        // Given
        Long fileId = 1L;
        given(parsedDataService.getParsedDataByFileId(fileId))
            .willReturn(mockParsedData);
        given(parsedDataService.getParsedDataAsMap(fileId))
            .willReturn(mockData);

        // When & Then
        mockMvc.perform(get("/api/parsed-data/{fileId}", fileId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.fileId").value(1))
            .andExpect(jsonPath("$.fileName").value("test.json"))
            .andExpect(jsonPath("$.fileType").value("JSON"))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data[0].name").value("John"));
    }

    @Test
    @DisplayName("파싱된 데이터 삭제 - 성공")
    void deleteParsedData_Success() throws Exception {
        // Given
        Long fileId = 1L;

        // When & Then
        mockMvc.perform(delete("/api/parsed-data/{fileId}", fileId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("파싱된 데이터 개수 조회 - 성공")
    void getParsedDataCount_Success() throws Exception {
        // Given
        long count = 5L;
        given(parsedDataService.getParsedDataCount())
            .willReturn(count);

        // When & Then
        mockMvc.perform(get("/api/parsed-data/count")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value(5));
    }
}

package com.core.data_pipeline_platform.domain.parse.service;

import com.core.data_pipeline_platform.domain.file.entity.FileEntity;
import com.core.data_pipeline_platform.domain.file.enums.FileType;
import com.core.data_pipeline_platform.domain.parse.entity.ParsedDataEntity;
import com.core.data_pipeline_platform.domain.parse.repository.ParsedDataRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ParsedDataService 테스트")
class ParsedDataServiceTest {

    @Mock
    private ParsedDataRepository parsedDataRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ParsedDataService parsedDataService;

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
    void getAllParsedData_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<ParsedDataEntity> entities = Arrays.asList(mockParsedData);
        Page<ParsedDataEntity> expectedPage = new PageImpl<>(entities, pageable, 1);

        given(parsedDataRepository.findAll(pageable))
            .willReturn(expectedPage);

        // When
        Page<ParsedDataEntity> result = parsedDataService.getAllParsedData(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
        assertThat(result.getTotalElements()).isEqualTo(1);

        then(parsedDataRepository).should().findAll(pageable);
    }

    @Test
    @DisplayName("파일 ID로 파싱된 데이터 조회 - 성공")
    void getParsedDataByFileId_Success() {
        // Given
        Long fileId = 1L;
        given(parsedDataRepository.findByFileId(fileId))
            .willReturn(Optional.of(mockParsedData));

        // When
        ParsedDataEntity result = parsedDataService.getParsedDataByFileId(fileId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getFile().getId()).isEqualTo(1L);

        then(parsedDataRepository).should().findByFileId(fileId);
    }

    @Test
    @DisplayName("파일 ID로 파싱된 데이터 조회 - 데이터 없음")
    void getParsedDataByFileId_NotFound() {
        // Given
        Long fileId = 999L;
        given(parsedDataRepository.findByFileId(fileId))
            .willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> parsedDataService.getParsedDataByFileId(fileId))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> {
                ResponseStatusException responseEx = (ResponseStatusException) ex;
                assertThat(responseEx.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(responseEx.getReason()).isEqualTo("파싱된 데이터를 찾을 수 없습니다.");
            });

        then(parsedDataRepository).should().findByFileId(fileId);
    }

    @Test
    @DisplayName("파싱된 데이터를 Map으로 변환 - 성공")
    void getParsedDataAsMap_Success() throws Exception {
        // Given
        Long fileId = 1L;
        given(parsedDataRepository.findByFileId(fileId))
            .willReturn(Optional.of(mockParsedData));
        given(objectMapper.readValue(any(String.class), any(com.fasterxml.jackson.core.type.TypeReference.class)))
            .willReturn(mockData);

        // When
        List<Map<String, Object>> result = parsedDataService.getParsedDataAsMap(fileId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).get("name")).isEqualTo("John");
        assertThat(result.get(1).get("name")).isEqualTo("Jane");

        then(parsedDataRepository).should().findByFileId(fileId);
        then(objectMapper).should().readValue(any(String.class), any(com.fasterxml.jackson.core.type.TypeReference.class));
    }

    @Test
    @DisplayName("파싱된 데이터 삭제 - 성공")
    void deleteParsedData_Success() {
        // Given
        Long fileId = 1L;
        given(parsedDataRepository.findByFileId(fileId))
            .willReturn(Optional.of(mockParsedData));

        // When
        parsedDataService.deleteParsedData(fileId);

        // Then
        then(parsedDataRepository).should().findByFileId(fileId);
        then(parsedDataRepository).should().delete(mockParsedData);
    }

    @Test
    @DisplayName("파싱된 데이터 개수 조회 - 성공")
    void getParsedDataCount_Success() {
        // Given
        long expectedCount = 5L;
        given(parsedDataRepository.count())
            .willReturn(expectedCount);

        // When
        long result = parsedDataService.getParsedDataCount();

        // Then
        assertThat(result).isEqualTo(expectedCount);

        then(parsedDataRepository).should().count();
    }
}

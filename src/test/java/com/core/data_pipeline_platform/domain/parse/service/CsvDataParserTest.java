package com.core.data_pipeline_platform.domain.parse.service;

import com.core.data_pipeline_platform.domain.file.enums.FileType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CsvDataParser 테스트")
class CsvDataParserTest {

    private CsvDataParser csvDataParser;

    @BeforeEach
    void setUp() {
        csvDataParser = new CsvDataParser();
    }

    @Test
    @DisplayName("올바른 CSV 파싱 성공")
    void parseData_valid_success() {
        // Given
        String validCsv = "name,age\nJohn,30\nJane,25";
        InputStream inputStream = new ByteArrayInputStream(validCsv.getBytes());

        // When
        List<Map<String, Object>> result = csvDataParser.parseData(FileType.CSV, inputStream);

        // Then
        assertNotNull(result);

        assertEquals(2, result.size());
        assertEquals("John", result.getFirst().get("name"));
        assertEquals(String.valueOf(30), result.getFirst().get("age"));

        assertEquals("Jane", result.getLast().get("name"));
        assertEquals(String.valueOf(25), result.getLast().get("age"));
    }

    @Test
    @DisplayName("컬럼 수가 맞지 않은 CSV 파싱 - 뛰어넘음")
    void parseData_invalid_success() {
        // Given
        String invalidCsv = "name,age,timestamp\nJohn,30\nJane,25,2025-09-21 04:16"; // 부족한 컬럼
        InputStream inputStream = new ByteArrayInputStream(invalidCsv.getBytes());

        // When
        List<Map<String, Object>> result = csvDataParser.parseData(FileType.CSV, inputStream);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());

        assertEquals("Jane", result.getFirst().get("name"));
        assertEquals(String.valueOf(25), result.getFirst().get("age"));
        assertEquals("2025-09-21 04:16", result.getFirst().get("timestamp"));
    }

    @Test
    @DisplayName("빈 파일 - 예외 발생")
    void parseData_emptyFile_throwsException() {
        // Given
        String emptyCsv = "";

        // When & Then
        assertThrows(ResponseStatusException.class,
                () -> csvDataParser.parseData(FileType.CSV, new ByteArrayInputStream(emptyCsv.getBytes())));
    }

    @Test
    @DisplayName("쉼표가 포함된 데이터")
    void parseData_withCommaInData_success() {
        String validCsv = "name,description\nJohn,\"Hello, World\"";

        InputStream inputStream = new ByteArrayInputStream(validCsv.getBytes());
        List<Map<String, Object>> result = csvDataParser.parseData(FileType.CSV, inputStream);

        assertEquals(1, result.size());
        assertEquals("John", result.getFirst().get("name"));
        assertEquals("Hello, World", result.getFirst().get("description"));

        System.out.println(result);
    }
}
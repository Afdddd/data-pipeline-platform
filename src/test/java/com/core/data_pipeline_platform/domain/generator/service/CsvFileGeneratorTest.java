package com.core.data_pipeline_platform.domain.generator.service;

import com.core.data_pipeline_platform.domain.generator.dto.GenerateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CsvFileGeneratorTest")
class CsvFileGeneratorTest {

    private CsvFileGenerator csvFileGenerator;

    @BeforeEach
    void setUp() {
        csvFileGenerator = new CsvFileGenerator();
    }

    @Test
    @DisplayName("정상적인 CSV 파일 생성")
    void generateFile_Success() {
        // Given
        GenerateRequest request = new GenerateRequest("test", 3);

        // When
        byte[] result = csvFileGenerator.generateFile(request);

        // Then
        assertNotNull(result, "결과가 null이 아니어야 함");
        assertTrue(result.length > 0, "결과가 비어있지 않아야 함");

        String csvString = new String(result, StandardCharsets.UTF_8);
        System.out.println("Generated CSV:\n" + csvString); // 디버깅용
    }

    @Test
    @DisplayName("생성된 CSV가 올바른 헤더를 가지는지 확인")
    void generateFile_ShouldHaveCorrectHeaders() {
        // Given
        GenerateRequest request = new GenerateRequest("test", 1);

        // When
        byte[] result = csvFileGenerator.generateFile(request);

        // Then
        String csvString = new String(result, StandardCharsets.UTF_8);
        String[] lines = csvString.split("\n");

        assertTrue(lines.length >= 1, "최소 헤더 라인은 있어야 함");

        String headerLine = lines[0];
        assertTrue(headerLine.contains("sensorId"), "sensorId 헤더 포함");
        assertTrue(headerLine.contains("value"), "value 헤더 포함");
        assertTrue(headerLine.contains("timestamp"), "timestamp 헤더 포함");
        assertTrue(headerLine.contains("status"), "status 헤더 포함");
    }

    @Test
    @DisplayName("요청한 레코드 수만큼 데이터 행이 생성되는지 확인")
    void generateFile_ShouldGenerateCorrectNumberOfRecords() {
        // Given
        int expectedRecordCount = 5;
        GenerateRequest request = new GenerateRequest("test", expectedRecordCount);

        // When
        byte[] result = csvFileGenerator.generateFile(request);

        // Then
        String csvString = new String(result, StandardCharsets.UTF_8);
        String[] lines = csvString.split("\n");

        // 헤더(1) + 데이터 행(expectedRecordCount) = 총 라인 수
        int expectedTotalLines = expectedRecordCount + 1;
        assertEquals(expectedTotalLines, lines.length,
                "헤더 + 데이터 행 수가 일치해야 함");
    }

    @Test
    @DisplayName("CSV 데이터 행의 형식이 올바른지 확인")
    void generateFile_ShouldHaveCorrectDataFormat() {
        // Given
        GenerateRequest request = new GenerateRequest("test", 2);

        // When
        byte[] result = csvFileGenerator.generateFile(request);

        // Then
        String csvString = new String(result, StandardCharsets.UTF_8);
        String[] lines = csvString.split("\n");

        // 첫 번째 데이터 행 검증 (인덱스 1)
        String firstDataLine = lines[1];
        String[] columns = firstDataLine.split(",");

        assertEquals(4, columns.length, "4개의 컬럼이 있어야 함");

        // 각 컬럼 검증
        assertTrue(columns[0].startsWith("SENSOR_"), "첫 번째 컬럼은 SENSOR_로 시작해야 함");
        assertDoesNotThrow(() -> Double.parseDouble(columns[1]),
                "두 번째 컬럼은 숫자여야 함 (value)");
        assertNotNull(columns[2], "세 번째 컬럼은 null이 아니어야 함 (timestamp)");
        assertEquals("Status", columns[3], "네 번째 컬럼은 'Status'여야 함");
    }

    @Test
    @DisplayName("CSV 형식이 표준을 준수하는지 확인")
    void generateFile_ShouldFollowCsvStandard() {
        // Given
        GenerateRequest request = new GenerateRequest("test", 3);

        // When
        byte[] result = csvFileGenerator.generateFile(request);

        // Then
        String csvString = new String(result, StandardCharsets.UTF_8);

        // CSV 기본 형식 검증
        assertTrue(csvString.contains(","), "쉼표 구분자가 포함되어야 함");
        assertTrue(csvString.contains("\n"), "줄바꿈이 포함되어야 함");

        String[] lines = csvString.split("\n");
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                String[] columns = line.split(",");
                assertTrue(columns.length > 0, "각 라인은 최소 1개 컬럼을 가져야 함");
            }
        }
    }
}

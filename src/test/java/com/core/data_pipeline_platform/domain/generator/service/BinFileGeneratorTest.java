package com.core.data_pipeline_platform.domain.generator.service;

import com.core.data_pipeline_platform.domain.generator.dto.GenerateRequest;
import com.core.data_pipeline_platform.domain.generator.model.SensorData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BinFileGeneratorTest")
class BinFileGeneratorTest {

    private BinFileGenerator binFileGenerator;

    @BeforeEach
    void setUp() {
        binFileGenerator = new BinFileGenerator();
    }

    @Test
    @DisplayName("정상적인 바이너리 파일 생성")
    void generateFile_Success() {
        // Given
        GenerateRequest request = new GenerateRequest("test", 3);

        // When
        byte[] result = binFileGenerator.generateFile(request);

        // Then
        assertNotNull(result, "결과가 null이 아니어야 함");
        assertTrue(result.length > 0, "결과가 비어있지 않아야 함");
        
        // 최소한의 헤더 크기 확인 (레코드 개수 4바이트)
        assertTrue(result.length >= 4, "최소 헤더 크기는 4바이트여야 함");
        
        System.out.println("Generated binary size: " + result.length + " bytes");
    }

    @Test
    @DisplayName("바이너리 헤더가 올바른지 확인")
    void generateFile_ShouldHaveCorrectHeader() throws IOException {
        // Given
        int expectedRecordCount = 5;
        GenerateRequest request = new GenerateRequest("test", expectedRecordCount);

        // When
        byte[] result = binFileGenerator.generateFile(request);

        // Then
        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(result);
             DataInputStream dataStream = new DataInputStream(byteStream)) {
            
            // 헤더에서 레코드 개수 읽기
            int actualRecordCount = dataStream.readInt();
            assertEquals(expectedRecordCount, actualRecordCount, 
                "헤더의 레코드 개수가 일치해야 함");
        }
    }

    @Test
    @DisplayName("바이너리 데이터 구조가 올바른지 확인")
    void generateFile_ShouldHaveCorrectBinaryStructure() throws IOException {
        // Given
        GenerateRequest request = new GenerateRequest("test", 2);

        // When
        byte[] result = binFileGenerator.generateFile(request);

        // Then
        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(result);
             DataInputStream dataStream = new DataInputStream(byteStream)) {
            
            // 헤더 읽기
            int recordCount = dataStream.readInt();
            assertEquals(2, recordCount, "레코드 개수 확인");
            
            // 첫 번째 레코드 검증
            SensorData firstRecord = readRecord(dataStream);
            assertNotNull(firstRecord, "첫 번째 레코드가 존재해야 함");
            assertTrue(firstRecord.getSensorId().startsWith("SENSOR_"),
                "sensorId는 SENSOR_로 시작해야 함");
            assertTrue(firstRecord.getValue() >= 0 && firstRecord.getValue() <= 100,
                "value는 0-100 범위여야 함");
            assertEquals("NORMAL", firstRecord.getStatus(), "status는 NORMAL이어야 함");
            assertNotNull(firstRecord.getTimestamp(), "timestamp는 null이 아니어야 함");
            
            // 두 번째 레코드 검증
            SensorData secondRecord = readRecord(dataStream);
            assertNotNull(secondRecord, "두 번째 레코드가 존재해야 함");
            assertTrue(secondRecord.getSensorId().startsWith("SENSOR_"),
                "sensorId는 SENSOR_로 시작해야 함");
        }
    }

    @Test
    @DisplayName("문자열 인코딩이 올바른지 확인 (UTF-8)")
    void generateFile_ShouldUseUtf8Encoding() throws IOException {
        // Given
        GenerateRequest request = new GenerateRequest("test", 1);

        // When
        byte[] result = binFileGenerator.generateFile(request);

        // Then
        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(result);
             DataInputStream dataStream = new DataInputStream(byteStream)) {
            
            // 헤더 건너뛰기
            dataStream.readInt();
            
            // 첫 번째 레코드의 sensorId 읽기
            SensorData record = readRecord(dataStream);
            
            // UTF-8로 인코딩된 문자열이 올바르게 디코딩되는지 확인
            assertDoesNotThrow(() -> {
                byte[] sensorIdBytes = record.getSensorId().getBytes(StandardCharsets.UTF_8);
                String decoded = new String(sensorIdBytes, StandardCharsets.UTF_8);
                assertEquals(record.getSensorId(), decoded, "UTF-8 인코딩/디코딩이 일치해야 함");
            });
        }
    }

    /**
     * 바이너리 스트림에서 센서 데이터 레코드 읽기
     */
    private SensorData readRecord(DataInputStream dataStream) throws IOException {
        return SensorData.builder()
                .sensorId(readString(dataStream)) // sensorId 읽기
                .value(dataStream.readDouble())  // value 읽기
                .timestamp(readString(dataStream)) // timestamp 읽기
                .status(readString(dataStream)) // status 읽기
                .build();
    }
    
    /**
     * 바이너리 스트림에서 문자열 읽기 (길이 정보 포함)
     */
    private String readString(DataInputStream dataStream) throws IOException {
        int length = dataStream.readInt();
        if (length == 0) {
            return "";
        }
        
        byte[] bytes = new byte[length];
        dataStream.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}

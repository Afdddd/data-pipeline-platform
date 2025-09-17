package com.core.data_pipeline_platform.domain.generator.service;

import com.core.data_pipeline_platform.domain.generator.dto.GenerateRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JsonFileGeneratorTest")
class JsonFileGeneratorTest {

    private JsonFileGenerator jsonFileGenerator;

    @BeforeEach
    void setUp() {
        jsonFileGenerator = new JsonFileGenerator(new ObjectMapper());
    }

    @Test
    @DisplayName("정상적인 JSON 파일 생성")
    void generateFile_Success() {
        // Given
        GenerateRequest request = new GenerateRequest("test", 5);

        // When
        byte[] result = jsonFileGenerator.generatorFile(request);

        // Then
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    @DisplayName("생성된 응답이 유효한 JSON 형식인지 확인")
    void generateFile_ShouldReturnValidJson() {
        // Given
        GenerateRequest request = new GenerateRequest("test", 3);

        // When
        byte[] result = jsonFileGenerator.generatorFile(request);

        // Then
        String jsonString = new String(result, StandardCharsets.UTF_8);
        
        // JSON 파싱 테스트
        assertDoesNotThrow(() -> {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(jsonString);
            
            assertTrue(jsonNode.isArray(), "JSON 배열이어야 함");
            assertEquals(3, jsonNode.size(), "요청한 레코드 수와 일치");
            
            // 첫 번째 요소 검증
            JsonNode firstElement = jsonNode.get(0);
            assertTrue(firstElement.has("sensorId"), "sensorId 필드 존재");
            assertTrue(firstElement.has("value"), "value 필드 존재");
            assertTrue(firstElement.has("timestamp"), "timestamp 필드 존재");
            assertTrue(firstElement.has("status"), "status 필드 존재");
            
        }, "유효한 JSON 형식이어야 함");
    }
}
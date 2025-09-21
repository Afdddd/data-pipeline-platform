package com.core.data_pipeline_platform.domain.parse.service;

import com.core.data_pipeline_platform.domain.file.enums.FileType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JsonDataParser 테스트")
class JsonDataParserTest {

    private JsonDataParser jsonDataParser;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        jsonDataParser = new JsonDataParser(objectMapper);
    }

    @Test
    @DisplayName("올바른 JSON 배열 파싱 성공")
    void parseData_validJsonArray_success() {
        // Given
        String validJson = "[{\"name\":\"John\",\"age\":30},{\"name\":\"Jane\",\"age\":25}]";
        InputStream inputStream = new ByteArrayInputStream(validJson.getBytes());

        // When
        List<Map<String, Object>> result = jsonDataParser.parseData(FileType.JSON, inputStream);

        // Then
        assertThat(result).hasSize(2);
        
        Map<String, Object> firstPerson = result.get(0);
        assertThat(firstPerson.get("name")).isEqualTo("John");
        assertThat(firstPerson.get("age")).isEqualTo(30);
        
        Map<String, Object> secondPerson = result.get(1);
        assertThat(secondPerson.get("name")).isEqualTo("Jane");
        assertThat(secondPerson.get("age")).isEqualTo(25);
    }

    @Test
    @DisplayName("단일 객체 JSON 파싱 성공")
    void parseData_singleObjectJson_success() {
        // Given
        String singleObjectJson = "[{\"name\":\"John\",\"age\":30}]";
        InputStream inputStream = new ByteArrayInputStream(singleObjectJson.getBytes());

        // When
        List<Map<String, Object>> result = jsonDataParser.parseData(FileType.JSON, inputStream);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("name")).isEqualTo("John");
        assertThat(result.get(0).get("age")).isEqualTo(30);
    }

    @Test
    @DisplayName("복잡한 JSON 구조 파싱 성공")
    void parseData_complexJsonStructure_success() {
        // Given
        String complexJson = "[{" +
                "\"name\":\"John\"," +
                "\"age\":30," +
                "\"address\":{\"city\":\"Seoul\",\"zipcode\":\"12345\"}," +
                "\"hobbies\":[\"reading\",\"swimming\"]" +
                "}]";
        InputStream inputStream = new ByteArrayInputStream(complexJson.getBytes());

        // When
        List<Map<String, Object>> result = jsonDataParser.parseData(FileType.JSON, inputStream);

        // Then
        assertThat(result).hasSize(1);
        Map<String, Object> person = result.get(0);
        assertThat(person.get("name")).isEqualTo("John");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> address = (Map<String, Object>) person.get("address");
        assertThat(address.get("city")).isEqualTo("Seoul");
        
        @SuppressWarnings("unchecked")
        List<String> hobbies = (List<String>) person.get("hobbies");
        assertThat(hobbies).containsExactly("reading", "swimming");
    }

    @Test
    @DisplayName("잘못된 JSON 형식 - ResponseStatusException 발생")
    void parseData_invalidJson_throwsResponseStatusException() {
        // Given
        String invalidJson = "[{\"name\":\"John\",\"age\":}]"; // 잘못된 JSON
        InputStream inputStream = new ByteArrayInputStream(invalidJson.getBytes());

        // When & Then
        assertThatThrownBy(() -> jsonDataParser.parseData(FileType.JSON, inputStream))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("비어있는 문자열 - ResponseStatusException 발생")
    void parseData_emptyString_throwsResponseStatusException() {
        // Given
        String emptyString = "";
        InputStream inputStream = new ByteArrayInputStream(emptyString.getBytes());

        // When & Then
        assertThatThrownBy(() -> jsonDataParser.parseData(FileType.JSON, inputStream))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("배열이 아닌 단일 객체 JSON - ResponseStatusException 발생")
    void parseData_singleObjectNotArray_throwsResponseStatusException() {
        // Given
        String singleObject = "{\"name\":\"John\",\"age\":30}"; // 배열이 아닌 단일 객체
        InputStream inputStream = new ByteArrayInputStream(singleObject.getBytes());

        // When & Then
        assertThatThrownBy(() -> jsonDataParser.parseData(FileType.JSON, inputStream))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("다양한 데이터 타입 파싱 성공")
    void parseData_variousDataTypes_success() {
        // Given
        String jsonWithTypes = "[{" +
                "\"stringValue\":\"text\"," +
                "\"intValue\":42," +
                "\"doubleValue\":3.14," +
                "\"booleanValue\":true," +
                "\"nullValue\":null" +
                "}]";
        InputStream inputStream = new ByteArrayInputStream(jsonWithTypes.getBytes());

        // When
        List<Map<String, Object>> result = jsonDataParser.parseData(FileType.JSON, inputStream);

        // Then
        assertThat(result).hasSize(1);
        Map<String, Object> data = result.get(0);
        assertThat(data.get("stringValue")).isEqualTo("text");
        assertThat(data.get("intValue")).isEqualTo(42);
        assertThat(data.get("doubleValue")).isEqualTo(3.14);
        assertThat(data.get("booleanValue")).isEqualTo(true);
        assertThat(data.get("nullValue")).isNull();
    }

}

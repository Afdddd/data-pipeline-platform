package com.core.data_pipeline_platform.domain.parse.service;

import com.core.data_pipeline_platform.domain.file.enums.FileType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


@DisplayName("XmlDataParser 테스트")
class XmlDataParserTest {

    private XmlDataParser xmlDataParser;

    @BeforeEach
    void setUp() {
        xmlDataParser = new XmlDataParser();
    }

    @Test
    @DisplayName("올바른 xml 파일 파싱 성공")
    void parseData_validXmlFile_success() {

        // Given
        String xmlBuilder = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<sensorData>\n" +
                "    <sensor>\n" +
                "        <sensorId>SENSOR_1</sensorId>\n" +
                "        <value>10.0</value>\n" +
                "        <timestamp>2025-09-21T08:10:10</timestamp>\n" +
                "        <status>NORMAL</status>\n" +
                "    </sensor>\n" +
                "</sensorData>\n";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(xmlBuilder.getBytes());

        // When
        List<Map<String, Object>> result = xmlDataParser.parseData(FileType.XML, inputStream);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("SENSOR_1", result.getFirst().get("sensorId"));
        assertEquals(String.valueOf(10.0), result.getFirst().get("value"));
        assertEquals("2025-09-21T08:10:10", result.getFirst().get("timestamp"));
        assertEquals("NORMAL", result.getFirst().get("status"));
    }

    @Test
    @DisplayName("태그에 빈 값 파싱 - 성공")
    void parseData_emptyTagValue_success() {

        // Given
        String xmlBuilder = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<sensorData>\n" +
                "    <sensor>\n" +
                "        <sensorId>SENSOR_1</sensorId>\n" +
                "        <value></value>\n" +
                "        <timestamp></timestamp>\n" +
                "        <status></status>\n" +
                "    </sensor>\n" +
                "</sensorData>\n";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(xmlBuilder.getBytes());

        // When
        List<Map<String, Object>> resultList = xmlDataParser.parseData(FileType.XML, inputStream);

        // Then
        assertNotNull(resultList);
        assertEquals(1, resultList.size());
        assertEquals("SENSOR_1", resultList.getFirst().get("sensorId"));
        assertEquals("", resultList.getFirst().get("value"));
        assertEquals("", resultList.getFirst().get("timestamp"));
        assertEquals("", resultList.getFirst().get("status"));
    }

    @Test
    @DisplayName("잘못된 루트 태그 - 예외 발생")
    void parseData_invalidRootTag_throwsException() {
        // Given
        String invalidXml =
                "<wrongRoot>\n" +
                "    <sensor>\n" +
                "        <sensorId>SENSOR_1</sensorId>\n" +
                "        <value>50.0</value>\n" +
                "        <timestamp>2023-01-01T00:00:00</timestamp>\n" +
                "        <status>NORMAL</status>\n" +
                "    </sensor>\n" +
                "</wrongRoot>";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(invalidXml.getBytes());

        // When & Then
        assertThrows(ResponseStatusException.class, () -> {
            xmlDataParser.parseData(FileType.XML, inputStream);
        });
    }
    
    @Test
    @DisplayName("sensor 데이터 없음 - 예외 발생")
    void parseData_noSensorData_throwsException() {
        // Given
        String emptySensorData = "<sensorData></sensorData>";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(emptySensorData.getBytes());

        // When & Then
        assertThrows(ResponseStatusException.class, () -> {
            xmlDataParser.parseData(FileType.XML, inputStream);
        });
    }

}
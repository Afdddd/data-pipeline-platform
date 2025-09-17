package com.core.data_pipeline_platform.domain.generator.service;

import com.core.data_pipeline_platform.domain.generator.dto.GenerateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("XmlFileGeneratorTest")
class XmlFileGeneratorTest {

    private XmlFileGenerator xmlFileGenerator;

    @BeforeEach
    void setUp() {
        xmlFileGenerator = new XmlFileGenerator();
    }

    @Test
    @DisplayName("정상적인 XML 파일 생성")
    void generateFile_Success() {
        // Given
        GenerateRequest request = new GenerateRequest("test", 3);

        // When
        byte[] result = xmlFileGenerator.generatorFile(request);

        // Then
        assertNotNull(result, "결과가 null이 아니어야 함");
        assertTrue(result.length > 0, "결과가 비어있지 않아야 함");

        String xmlString = new String(result, StandardCharsets.UTF_8);
        System.out.println("Generated XML:\n" + xmlString); // 디버깅용
    }

    @Test
    @DisplayName("생성된 XML이 올바른 형식인지 확인")
    void generateFile_ShouldBeValidXml() {
        // Given
        GenerateRequest request = new GenerateRequest("test", 2);

        // When
        byte[] result = xmlFileGenerator.generatorFile(request);

        // Then
        String xmlString = new String(result, StandardCharsets.UTF_8);

        // XML 파싱 테스트 (예외가 발생하지 않으면 유효한 XML)
        assertDoesNotThrow(() -> {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(result));

            assertNotNull(document, "XML 문서가 파싱되어야 함");
        }, "유효한 XML 형식이어야 함");
    }

    @Test
    @DisplayName("XML 구조가 올바른지 확인")
    void generateFile_ShouldHaveCorrectStructure() throws Exception {
        // Given
        GenerateRequest request = new GenerateRequest("test", 3);

        // When
        byte[] result = xmlFileGenerator.generatorFile(request);

        // Then
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new ByteArrayInputStream(result));

        // 루트 엘리먼트 검증
        Element root = document.getDocumentElement();
        assertEquals("sensorData", root.getTagName(), "루트 엘리먼트는 sensorData여야 함");

        // sensor 엘리먼트 개수 검증
        NodeList sensors = document.getElementsByTagName("sensor");
        assertEquals(3, sensors.getLength(), "요청한 레코드 수와 일치해야 함");

        // 첫 번째 센서 데이터 검증
        Element firstSensor = (Element) sensors.item(0);
        assertNotNull(firstSensor, "첫 번째 센서 엘리먼트가 존재해야 함");

        // 필수 필드 검증
        NodeList sensorIds = firstSensor.getElementsByTagName("sensorId");
        NodeList values = firstSensor.getElementsByTagName("value");
        NodeList timestamps = firstSensor.getElementsByTagName("timestamp");
        NodeList statuses = firstSensor.getElementsByTagName("status");

        assertEquals(1, sensorIds.getLength(), "sensorId 필드가 하나 있어야 함");
        assertEquals(1, values.getLength(), "value 필드가 하나 있어야 함");
        assertEquals(1, timestamps.getLength(), "timestamp 필드가 하나 있어야 함");
        assertEquals(1, statuses.getLength(), "status 필드가 하나 있어야 함");
    }

    @Test
    @DisplayName("XML 기본 구조 검증")
    void generateFile_ShouldHaveXmlDeclaration() {
        // Given
        GenerateRequest request = new GenerateRequest("test", 1);

        // When
        byte[] result = xmlFileGenerator.generatorFile(request);

        // Then
        String xmlString = new String(result, StandardCharsets.UTF_8);

        assertTrue(xmlString.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"),
                "XML 선언부가 있어야 함");
        assertTrue(xmlString.contains("<sensorData>"), "루트 엘리먼트 시작 태그가 있어야 함");
        assertTrue(xmlString.contains("</sensorData>"), "루트 엘리먼트 종료 태그가 있어야 함");
    }
}
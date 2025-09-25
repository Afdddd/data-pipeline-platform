package com.core.data_pipeline_platform.domain.parse.service;

import com.core.data_pipeline_platform.domain.file.enums.FileType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class XmlDataParser implements DataParser {

    private static final String SENSOR_ID = "sensorId";
    private static final String VALUE = "value";
    private static final String TIMESTAMP = "timestamp";
    private static final String STATUS = "status";

    @Override
    public List<Map<String, Object>> parseData(FileType fileType, InputStream inputStream) {

        if (fileType != FileType.XML) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일 타입 불일치: XML이어야 합니다.");
        }

        if (inputStream == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "입력 스트림이 null 입니다.");
        }

        List<Map<String, Object>> records = new ArrayList<>();

        try {
            // DOM 파서 생성
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);
            
            // 루트 태그 검증
            Element root = document.getDocumentElement();
            if (!"sensorData".equals(root.getTagName())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "올바른 XML 형식이 아닙니다. 루트 태그는 'sensorData'여야 합니다.");
            }
            
            // sensor 요소들 찾기
            NodeList sensorNodes = document.getElementsByTagName("sensor");
            
            // sensor 데이터 존재 검증
            if (sensorNodes.getLength() == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sensor 데이터가 없습니다.");
            }

            // 각 sensor 요소를 Map으로 변환
            for (int i = 0; i < sensorNodes.getLength(); i++) {
                Element sensorElement = (Element) sensorNodes.item(i);
                Map<String, Object> record = new HashMap<>();

                record.put(SENSOR_ID, getElementContent(sensorElement, SENSOR_ID));
                record.put(VALUE, getElementContent(sensorElement, VALUE));
                record.put(TIMESTAMP, getElementContent(sensorElement, TIMESTAMP));
                record.put(STATUS, getElementContent(sensorElement, STATUS));

                records.add(record);
            }
            
            return records;
            
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "XML 파싱 실패", e);
        }
    }

    @Override
    public FileType getSupportedFileType() {
        return FileType.XML;
    }

    private Object getElementContent(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return "";
        }
        return nodes.item(0).getTextContent();
    }
}
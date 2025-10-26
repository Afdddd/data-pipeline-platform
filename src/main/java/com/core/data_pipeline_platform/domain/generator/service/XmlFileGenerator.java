package com.core.data_pipeline_platform.domain.generator.service;

import com.core.data_pipeline_platform.domain.file.enums.FileType;
import com.core.data_pipeline_platform.domain.generator.dto.GenerateRequest;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Component
public class XmlFileGenerator implements FileGenerator{

    @Override
    public byte[] generateFile(GenerateRequest request) {
        StringBuilder xmlBuilder = new StringBuilder();

        // XML 선언부
        xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xmlBuilder.append("<sensorData>\n");

        // 센서 데이터 생성
        for (int i = 0; i < request.recordCount(); i++) {
            xmlBuilder.append("    <sensor>\n");
            xmlBuilder.append("        <sensorId>SENSOR_").append(i).append("</sensorId>\n");
            xmlBuilder.append("        <value>").append(String.format("%.2f", Math.random() * 100)).append("</value>\n");
            xmlBuilder.append("        <timestamp>").append(LocalDateTime.now()).append("</timestamp>\n");
            xmlBuilder.append("        <status>NORMAL</status>\n");
            xmlBuilder.append("    </sensor>\n");
        }

        xmlBuilder.append("</sensorData>");

        return xmlBuilder.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public FileType getSupportedFileType() {
        return FileType.XML;
    }
}
package com.core.data_pipeline_platform.domain.generator.service;

import com.core.data_pipeline_platform.domain.file.enums.FileType;
import com.core.data_pipeline_platform.domain.generator.dto.GenerateRequest;
import com.core.data_pipeline_platform.domain.generator.model.SensorData;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Component
public class CsvFileGenerator implements FileGenerator{

    @Override
    public byte[] generateFile(GenerateRequest request) {

        StringBuilder csvBuilder = new StringBuilder();
        Field[] fields = SensorData.class.getDeclaredFields();

        for (int i = 0; i < fields.length; i++) {
            csvBuilder.append(fields[i].getName());
            if (i < fields.length - 1) {
                csvBuilder.append(",");
            }
        }
        csvBuilder.append("\n");


        for (int i = 0; i < request.recordCount(); i++) {
            csvBuilder.append("SENSOR_")
                    .append(i)
                    .append(",")
                    .append(Math.random()*100)
                    .append(",")
                    .append(LocalDateTime.now())
                    .append(",")
                    .append("Status")
                    .append("\n");

        }

        return csvBuilder.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public FileType getSupportedFileType() {
        return FileType.CSV;
    }
}

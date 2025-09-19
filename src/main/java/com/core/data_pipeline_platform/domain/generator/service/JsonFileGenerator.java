package com.core.data_pipeline_platform.domain.generator.service;

import com.core.data_pipeline_platform.domain.generator.dto.GenerateRequest;
import com.core.data_pipeline_platform.domain.generator.model.SensorData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class JsonFileGenerator implements FileGenerator{

    private final ObjectMapper objectMapper;

    public JsonFileGenerator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] generatorFile(GenerateRequest request) {
        List<SensorData> sensorDataList = new ArrayList<>();

        for (int i = 0; i < request.recordCount(); i++) {
            sensorDataList.add(SensorData.builder()
                    .sensorId("SENSOR_"+i)
                    .value(Math.random()*100)
                    .timestamp(LocalDateTime.now().toString())
                    .status("NORMAL")
                    .build());
        }

        try{
            return objectMapper.writeValueAsBytes(sensorDataList);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 생성 실패", e);
        }

    }
}

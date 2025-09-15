package com.core.data_pipeline_platform.domain.generator.service;

import com.core.data_pipeline_platform.domain.generator.dto.GenerateRequest;
import com.core.data_pipeline_platform.domain.generator.model.SensorData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class JsonFileGenerator {

    private final ObjectMapper objectMapper;

    public JsonFileGenerator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public byte[] generatorFile(GenerateRequest request) {
        List<SensorData> sensorDataList = new ArrayList<>();

        for (int i = 0; i < request.recordCound(); i++) {
            sensorDataList.add(SensorData.builder()
                    .sensorId("SENSOR_"+i)
                    .value(Math.random()*100)
                    .timestamp(LocalDateTime.now().toString())
                    .status("NORMAL")
                    .build());
        }

        try{
            String json = objectMapper.writeValueAsString(sensorDataList);
            return json.getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 생성 실패", e);
        }

    }
}

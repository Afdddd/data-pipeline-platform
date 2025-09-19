package com.core.data_pipeline_platform.domain.generator.service;

import com.core.data_pipeline_platform.domain.generator.dto.GenerateRequest;
import com.core.data_pipeline_platform.domain.generator.model.SensorData;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * 바이너리 센서 데이터 파일 생성기
 * 
 * 바이너리 구조:
 * [Header - 4 bytes]
 * ├── Record Count (int32)
 * 
 * [Record N - Variable length]
 * ├── sensorId length (int32): 4 bytes
 * ├── sensorId data (UTF-8): variable
 * ├── value (double): 8 bytes
 * ├── timestamp length (int32): 4 bytes
 * ├── timestamp data (UTF-8): variable
 * ├── status length (int32): 4 bytes
 * └── status data (UTF-8): variable
 */
@Component
public class BinFileGenerator implements FileGenerator{

    @Override
    public byte[] generatorFile(GenerateRequest request) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             DataOutputStream dataStream = new DataOutputStream(byteStream)) {
            
            // 헤더: 레코드 개수 (4바이트)
            dataStream.writeInt(request.recordCount());
            
            // 각 센서 데이터 레코드 생성
            for (int i = 0; i < request.recordCount(); i++) {
                SensorData sensorData = SensorData.builder()
                        .sensorId("SENSOR_" + i)
                        .value(Math.random() * 100)
                        .timestamp(LocalDateTime.now().toString())
                        .status("NORMAL")
                        .build();
                
                writeRecord(dataStream, sensorData);
            }
            
            dataStream.flush();
            return byteStream.toByteArray();
            
        } catch (IOException e) {
            throw new RuntimeException("바이너리 파일 생성 실패", e);
        }
    }
    
    /**
     * 개별 센서 데이터 레코드를 바이너리로 쓰기
     */
    private void writeRecord(DataOutputStream dataStream, SensorData sensorData) throws IOException {
        // sensorId: length + UTF-8 bytes
        writeString(dataStream, sensorData.getSensorId());
        
        // value: 8바이트 double
        dataStream.writeDouble(sensorData.getValue());
        
        // timestamp: length + UTF-8 bytes
        writeString(dataStream, sensorData.getTimestamp());
        
        // status: length + UTF-8 bytes  
        writeString(dataStream, sensorData.getStatus());
    }
    
    /**
     * 문자열을 바이너리로 쓰기 (길이 정보 포함)
     * Format: [length(4 bytes)][UTF-8 data(variable)]
     */
    private void writeString(DataOutputStream dataStream, String str) throws IOException {
        if (str == null) {
            dataStream.writeInt(0); // null인 경우 길이 0
            return;
        }
        
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        dataStream.writeInt(bytes.length); // 문자열 길이 (바이트 단위)
        dataStream.write(bytes);           // 실제 UTF-8 데이터
    }
}
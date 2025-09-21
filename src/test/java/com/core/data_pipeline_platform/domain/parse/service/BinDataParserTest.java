package com.core.data_pipeline_platform.domain.parse.service;

import com.core.data_pipeline_platform.domain.file.enums.FileType;
import com.core.data_pipeline_platform.domain.generator.dto.GenerateRequest;
import com.core.data_pipeline_platform.domain.generator.service.BinFileGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BinDataParser 테스트")
class BinDataParserTest {

    private BinDataParser binDataParser;
    private BinFileGenerator binFileGenerator;

    @BeforeEach
    void setUp() {
        binDataParser = new BinDataParser();
        binFileGenerator = new BinFileGenerator();
    }

    @Test
    @DisplayName("올바른 bin 파일 파싱 성공")
    void parseData_validBinFile_success() {
        // Given
        byte[] binData = binFileGenerator.generateFile(new GenerateRequest("test.bin", 2));

        // When
        ByteArrayInputStream inputStream = new ByteArrayInputStream(binData);
        List<Map<String, Object>> result = binDataParser.parseData(FileType.BIN, inputStream);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("빈 파일 - 예외 발생")
    void parseData_emptyFile_throwsException() {
        // Given
        byte[] emptyData = new byte[0];
        ByteArrayInputStream inputStream = new ByteArrayInputStream(emptyData);

        // When & Then
        assertThrows(ResponseStatusException.class, () -> {
            binDataParser.parseData(FileType.BIN, inputStream);
        });
    }

    @Test
    @DisplayName("헤더만 있는 파일 - 예외 발생")
    void parseData_headerOnly_throwsException() throws IOException {
        // Given
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(byteStream);
        dataStream.writeInt(1); // 레코드 수만 쓰고 끝
        dataStream.close();

        ByteArrayInputStream inputStream = new ByteArrayInputStream(byteStream.toByteArray());

        // When & Then
        assertThrows(ResponseStatusException.class, () -> {
            binDataParser.parseData(FileType.BIN, inputStream);
        });
    }

    @Test
    @DisplayName("불완전한 데이터 - 예외 발생")
    void parseData_incompleteData_throwsException() throws IOException {
        // Given: sensorId만 쓰고 나머지는 없음
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(byteStream);
        dataStream.writeInt(1); // 레코드 수
        // sensorId
        byte[] sensorBytes = "SENSOR_0".getBytes();
        dataStream.writeInt(sensorBytes.length);
        dataStream.write(sensorBytes);
        // value, timestamp, status는 없음!
        dataStream.close();

        ByteArrayInputStream inputStream = new ByteArrayInputStream(byteStream.toByteArray());

        // When & Then
        assertThrows(ResponseStatusException.class, () -> {
            binDataParser.parseData(FileType.BIN, inputStream);
        });
    }

}
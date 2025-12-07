package com.core.data_pipeline_platform.domain.parse.service;

import com.core.data_pipeline_platform.domain.file.enums.FileType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BinDataParser implements DataParser {

    private static final String SENSOR_ID = "sensorId";
    private static final String VALUE = "value";
    private static final String TIMESTAMP = "timestamp";
    private static final String STATUS = "status";

    @Override
    public List<Map<String, Object>> parseData(FileType fileType, InputStream inputStream) {

        if (fileType != FileType.BIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일 타입 불일치: BIN이어야 합니다.");
        }

        if (inputStream == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "입력 스트림이 null 입니다.");
        }

        List<Map<String, Object>> records = new ArrayList<>();

        try {
            DataInputStream dataStream = new DataInputStream(inputStream);
            int recordCount = dataStream.readInt();

            if (recordCount < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 레코드 수");
            }
            for (int i = 0; i < recordCount; i++) {
                Map<String, Object> record = new HashMap<>();
                record.put(SENSOR_ID, readString(dataStream));
                record.put(VALUE, dataStream.readDouble());
                record.put(TIMESTAMP, readString(dataStream));
                record.put(STATUS, readString(dataStream));
                records.add(record);
            }

            return records;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "BIN 파일 파싱 실패");
        }
    }

    @Override
    public FileType getSupportedFileType() {
        return FileType.BIN;
    }

    private String readString(DataInputStream dataStream) throws IOException {
        int length = dataStream.readInt();
        if (length == 0) return "";
        byte[] bytes = new byte[length];
        dataStream.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}

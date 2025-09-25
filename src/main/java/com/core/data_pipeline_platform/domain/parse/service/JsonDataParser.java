package com.core.data_pipeline_platform.domain.parse.service;

import com.core.data_pipeline_platform.domain.file.enums.FileType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JsonDataParser implements DataParser {

    private final ObjectMapper objectMapper;

    @Override
    public List<Map<String, Object>> parseData(FileType fileType, InputStream inputStream) {

        if (fileType != FileType.JSON) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일 타입 불일치: JSON이어야 합니다.");
        }

        if (inputStream == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "입력 스트림이 null 입니다.");
        }

        try {
            return objectMapper.readValue(
                    inputStream,
                    new TypeReference<>() {
                    }
            );
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Json 파싱 실패");
        }
    }

    @Override
    public FileType getSupportedFileType() {
        return FileType.JSON;
    }
}

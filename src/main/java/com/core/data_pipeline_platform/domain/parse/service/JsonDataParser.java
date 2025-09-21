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

        try {
            return objectMapper.readValue(
                    inputStream,
                    new TypeReference<List<Map<String, Object>>>() {
                    }
            );
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Json 파싱 실패");
        }
    }
}

package com.core.data_pipeline_platform.domain.parse.service;

import com.core.data_pipeline_platform.domain.file.entity.FileEntity;
import com.core.data_pipeline_platform.domain.file.enums.FileType;
import com.core.data_pipeline_platform.domain.parse.entity.ParsedDataEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DataParsingService {

    private final ParserFactory parserFactory;
    private final ObjectMapper objectMapper;

    public ParsedDataEntity parseAndSave(FileType fileType, InputStream inputStream, FileEntity file) {
        try {
            DataParser parser = parserFactory.getParser(fileType);
            List<Map<String, Object>> maps = parser.parseData(fileType, inputStream);

            String jsonData = objectMapper.writeValueAsString(maps);

            return ParsedDataEntity.builder()
                    .file(file)
                    .data(jsonData)
                    .build();

        }catch (JsonProcessingException e){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파싱 실패");
        }

    }



}

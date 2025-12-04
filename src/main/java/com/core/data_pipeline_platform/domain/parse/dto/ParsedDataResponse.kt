package com.core.data_pipeline_platform.domain.parse.dto;

import com.core.data_pipeline_platform.domain.file.entity.FileEntity;
import com.core.data_pipeline_platform.domain.parse.entity.ParsedDataEntity;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class ParsedDataResponse {
    private Long id;
    private Long fileId;
    private String fileName;
    private String fileType;
    private List<Map<String, Object>> data;

    public static ParsedDataResponse from(ParsedDataEntity entity, List<Map<String, Object>> parsedData) {
        FileEntity file = entity.getFile();
        return ParsedDataResponse.builder()
            .id(entity.getId())
            .fileId(file.getId())
            .fileName(file.getOriginName())
            .fileType(file.getFileType().name())
            .data(parsedData)
            .build();
    }
}
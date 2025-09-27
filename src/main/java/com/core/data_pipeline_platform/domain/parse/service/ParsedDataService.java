package com.core.data_pipeline_platform.domain.parse.service;

import com.core.data_pipeline_platform.domain.parse.entity.ParsedDataEntity;
import com.core.data_pipeline_platform.domain.parse.repository.ParsedDataRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ParsedDataService {

    private final ParsedDataRepository parsedDataRepository;
    private final ObjectMapper objectMapper;

    /**
     * 모든 파싱된 데이터 조회 (페이징)
     */
    public Page<ParsedDataEntity> getAllParsedData(Pageable pageable) {
        return parsedDataRepository.findAll(pageable);
    }

    /**
     * 파일 ID로 파싱된 데이터 조회
     */
    public ParsedDataEntity getParsedDataByFileId(Long fileId) {
        return parsedDataRepository.findByFileId(fileId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "파싱된 데이터를 찾을 수 없습니다."));
    }

    /**
     * 파싱된 데이터의 JSON 데이터를 List<Map>으로 변환하여 반환
     */
    public List<Map<String, Object>> getParsedDataAsMap(Long fileId) {
        ParsedDataEntity parsedData = getParsedDataByFileId(fileId);
        
        try {
            return objectMapper.readValue(parsedData.getData(), new TypeReference<>() {});
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "데이터 변환 실패");
        }
    }

    /**
     * 파싱된 데이터 삭제
     */
    public void deleteParsedData(Long fileId) {
        ParsedDataEntity parsedData = getParsedDataByFileId(fileId);
        parsedDataRepository.delete(parsedData);
    }

    /**
     * 파싱된 데이터 개수 조회
     */
    public long getParsedDataCount() {
        return parsedDataRepository.count();
    }
}

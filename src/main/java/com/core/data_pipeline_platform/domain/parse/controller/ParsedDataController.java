package com.core.data_pipeline_platform.domain.parse.controller;

import com.core.data_pipeline_platform.domain.parse.dto.ParsedDataResponse;
import com.core.data_pipeline_platform.domain.parse.entity.ParsedDataEntity;
import com.core.data_pipeline_platform.domain.parse.service.ParsedDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/parsed-data")
@RequiredArgsConstructor
public class ParsedDataController {

    private final ParsedDataService parsedDataService;

    /**
     * 모든 파싱된 데이터 조회 (페이징)
     */
    @GetMapping
    public ResponseEntity<Page<ParsedDataEntity>> getAllParsedData(Pageable pageable) {
        Page<ParsedDataEntity> parsedData = parsedDataService.getAllParsedData(pageable);
        return ResponseEntity.ok(parsedData);
    }

    /**
     * 파일 ID로 파싱된 데이터 조회
     */
    @GetMapping("/{fileId}")
    public ResponseEntity<ParsedDataResponse> getParsedDataByFileId(@PathVariable Long fileId) {
        ParsedDataEntity entity = parsedDataService.getParsedDataByFileId(fileId);
        List<Map<String, Object>> data = parsedDataService.getParsedDataAsMap(fileId);
        
        ParsedDataResponse response = ParsedDataResponse.from(entity, data);
        return ResponseEntity.ok(response);
    }

    /**
     * 파싱된 데이터 삭제
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteParsedData(@PathVariable Long fileId) {
        parsedDataService.deleteParsedData(fileId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 파싱된 데이터 개수 조회
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getParsedDataCount() {
        long count = parsedDataService.getParsedDataCount();
        return ResponseEntity.ok(count);
    }
}

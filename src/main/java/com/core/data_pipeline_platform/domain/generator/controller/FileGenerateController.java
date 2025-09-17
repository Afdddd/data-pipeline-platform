package com.core.data_pipeline_platform.domain.generator.controller;

import com.core.data_pipeline_platform.domain.generator.dto.GenerateRequest;
import com.core.data_pipeline_platform.domain.generator.service.CsvFileGenerator;
import com.core.data_pipeline_platform.domain.generator.service.JsonFileGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/generate")
@RequiredArgsConstructor
public class FileGenerateController {

    private final JsonFileGenerator jsonFileGenerator;
    private final CsvFileGenerator csvFileGenerator;

    @PostMapping(value = "/json")
    public ResponseEntity<byte[]> generateJsonFile(@RequestBody GenerateRequest request) {
        byte[] jsonContent = jsonFileGenerator.generatorFile(request);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + request.fileName() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(jsonContent.length))
                .body(jsonContent);
    }

    @PostMapping(value = "/csv")
    public ResponseEntity<byte[]> generateCsvFile(@RequestBody GenerateRequest request) {
        byte[] csvContent = csvFileGenerator.generatorFile(request);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + request.fileName() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(csvContent.length))
                .body(csvContent);
    }
}

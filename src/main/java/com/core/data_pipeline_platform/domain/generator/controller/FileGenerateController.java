package com.core.data_pipeline_platform.domain.generator.controller;

import com.core.data_pipeline_platform.domain.generator.dto.GenerateRequest;
import com.core.data_pipeline_platform.domain.generator.service.JsonFileGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/generate")
@RequiredArgsConstructor
public class FileGenerateController {

    private final JsonFileGenerator jsonFileGenerator;

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
}

package com.core.data_pipeline_platform.domain.generator.controller;

import com.core.data_pipeline_platform.domain.generator.dto.GenerateRequest;
import com.core.data_pipeline_platform.domain.generator.service.BinFileGenerator;
import com.core.data_pipeline_platform.domain.generator.service.CsvFileGenerator;
import com.core.data_pipeline_platform.domain.generator.service.JsonFileGenerator;
import com.core.data_pipeline_platform.domain.generator.service.XmlFileGenerator;
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
    private final XmlFileGenerator xmlFileGenerator;
    private final BinFileGenerator binFileGenerator;

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

    @PostMapping(value = "/xml")
    public ResponseEntity<byte[]> generateXmlFile(@RequestBody GenerateRequest request) {
        byte[] xmlContent = xmlFileGenerator.generatorFile(request);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + request.fileName() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, "application/xml")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(xmlContent.length))
                .body(xmlContent);
    }

    @PostMapping(value = "/bin")
    public ResponseEntity<byte[]> generateBinFile(@RequestBody GenerateRequest request) {
        byte[] binContent = binFileGenerator.generatorFile(request);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + request.fileName() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, "application/octet-stream")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(binContent.length))
                .body(binContent);
    }
}

package com.core.data_pipeline_platform.domain.generator.controller;

import com.core.data_pipeline_platform.domain.file.enums.FileType;
import com.core.data_pipeline_platform.domain.generator.dto.GenerateRequest;
import com.core.data_pipeline_platform.domain.generator.service.*;
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

    @PostMapping(value = "/{format}")
    public ResponseEntity<byte[]> generateFile(@PathVariable String format, @RequestBody GenerateRequest request ){
        FileGenerator fileGenerator = getGenerator(format);
        byte[] content = fileGenerator.generatorFile(request);
        String mimeType = getMimeType(format);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + request.fileName() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, mimeType)
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(content.length))
                .body(content);
    }

    private FileGenerator getGenerator(String format){
        return switch(format) {
            case "json" -> jsonFileGenerator;
            case "csv" -> csvFileGenerator;
            case "xml" -> xmlFileGenerator;
            case "bin" -> binFileGenerator;
            default -> throw new IllegalArgumentException("Unsupported format: " + format);
        };
    }

    private String getMimeType(String format) {
        return FileType.getMimeType(format);
    }
}

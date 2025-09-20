package com.core.data_pipeline_platform.domain.generator.controller;

import com.core.data_pipeline_platform.domain.file.enums.FileType;
import com.core.data_pipeline_platform.domain.generator.dto.GenerateRequest;
import com.core.data_pipeline_platform.domain.generator.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("api/generate")
@RequiredArgsConstructor
public class FileGenerateController {

    private final JsonFileGenerator jsonFileGenerator;
    private final CsvFileGenerator csvFileGenerator;
    private final XmlFileGenerator xmlFileGenerator;
    private final BinFileGenerator binFileGenerator;

    @PostMapping(value = "/{format}")
    public ResponseEntity<byte[]> generateFile(@PathVariable String format,
                                               @RequestBody @Valid GenerateRequest request ){
        FileGenerator fileGenerator = getGenerator(format);
        byte[] content = fileGenerator.generateFile(request);
        String mimeType = getMimeType(format);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + request.fileName() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, mimeType)
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(content.length))
                .body(content);
    }

    private FileGenerator getGenerator(String format){
        String f = format.toLowerCase();
        return switch(f) {
            case "json" -> jsonFileGenerator;
            case "csv" -> csvFileGenerator;
            case "xml" -> xmlFileGenerator;
            case "bin" -> binFileGenerator;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 형식입니다. "+"["+format+"]");
        };
    }

    private String getMimeType(String format) {
        return FileType.getMimeType(format);
    }
}

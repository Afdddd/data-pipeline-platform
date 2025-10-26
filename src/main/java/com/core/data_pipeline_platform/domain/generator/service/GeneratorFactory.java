package com.core.data_pipeline_platform.domain.generator.service;

import com.core.data_pipeline_platform.domain.file.enums.FileType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class GeneratorFactory {

    private final Map<FileType, FileGenerator> generators;

    public GeneratorFactory(List<FileGenerator> generatorList) {
        this.generators = generatorList.stream()
                .collect(Collectors.toMap(
                    FileGenerator::getSupportedFileType,
        fileGenerator -> fileGenerator
                ));

    }

    public FileGenerator getFileGenerator(FileType fileType) {
        FileGenerator fileGenerator = generators.get(fileType);
        if (fileGenerator == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 파일형식입니다.");
        }
        return fileGenerator;
    }
}

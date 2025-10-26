package com.core.data_pipeline_platform.domain.generator.service;

import com.core.data_pipeline_platform.domain.file.enums.FileType;
import com.core.data_pipeline_platform.domain.generator.dto.GenerateRequest;

public interface FileGenerator {
    byte[] generateFile(GenerateRequest request);

    FileType getSupportedFileType();
}

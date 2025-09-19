package com.core.data_pipeline_platform.domain.generator.service;

import com.core.data_pipeline_platform.domain.generator.dto.GenerateRequest;

public interface FileGenerator {
    byte[] generatorFile(GenerateRequest request);
}

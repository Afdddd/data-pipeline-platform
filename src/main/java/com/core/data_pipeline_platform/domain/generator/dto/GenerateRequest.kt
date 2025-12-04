package com.core.data_pipeline_platform.domain.generator.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record GenerateRequest(
        @NotBlank String fileName,
        @Min(1) @Max(1000) int recordCount) {
}

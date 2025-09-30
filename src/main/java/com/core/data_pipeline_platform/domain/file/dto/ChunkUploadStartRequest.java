package com.core.data_pipeline_platform.domain.file.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ChunkUploadStartRequest(
        @NotBlank String fileName,
        @Positive long totalSize,
        @Positive int totalChunks
) {}

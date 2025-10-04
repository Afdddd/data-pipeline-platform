package com.core.data_pipeline_platform.domain.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ChunkUploadRequest(
        @NotBlank String sessionId,
        @NotBlank int chunkIndex,
        @NotBlank byte[] chunkData,
        @Positive long chunkSize
) {}

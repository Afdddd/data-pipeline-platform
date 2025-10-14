package com.core.data_pipeline_platform.domain.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ChunkUploadRequest(
        @NotBlank String sessionId,
        @PositiveOrZero int chunkIndex,
        @NotNull byte[] chunkData,
        @PositiveOrZero long chunkSize
) {}

package com.core.data_pipeline_platform.domain.file.dto;

public record ChunkUploadCancelResponse(
        boolean success,
        String message
) {
}

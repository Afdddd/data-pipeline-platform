package com.core.data_pipeline_platform.domain.file.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

data class ChunkUploadStartRequest(
    val fileName: @NotBlank String?,
    val totalSize: @Positive Long,
    val totalChunks: @Positive Int
)

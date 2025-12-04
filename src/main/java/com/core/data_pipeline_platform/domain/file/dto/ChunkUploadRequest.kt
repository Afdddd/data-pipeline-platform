package com.core.data_pipeline_platform.domain.file.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero

data class ChunkUploadRequest(
    val sessionId: @NotBlank String?,
    val chunkIndex: @PositiveOrZero Int,
    val chunkData: ByteArray?,
    val chunkSize: @PositiveOrZero Long
)

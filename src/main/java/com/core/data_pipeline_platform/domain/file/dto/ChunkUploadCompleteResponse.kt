package com.core.data_pipeline_platform.domain.file.dto

data class ChunkUploadCompleteResponse(
    val success: Boolean,
    val message: String,
    val failedChunks: MutableList<Int>,  // 실패한 청크 인덱스들
    val fileId: String? // 성공 시 생성된 파일 ID
)

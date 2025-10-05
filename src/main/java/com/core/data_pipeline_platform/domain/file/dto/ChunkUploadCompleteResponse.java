package com.core.data_pipeline_platform.domain.file.dto;


import java.util.List;

public record ChunkUploadCompleteResponse(
        boolean success,
        String message,
        List<Integer> failedChunks,  // 실패한 청크 인덱스들
        String fileId  // 성공 시 생성된 파일 ID
) {}

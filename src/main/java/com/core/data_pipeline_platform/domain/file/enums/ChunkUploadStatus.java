package com.core.data_pipeline_platform.domain.file.enums;

/**
 * 청크 업로드 상태를 나타내는 enum
 */
public enum ChunkUploadStatus {
    PENDING,      // 대기 중
    IN_PROGRESS,  // 진행 중
    COMPLETED,    // 완료
    FAILED,       // 실패
    CANCELLED     // 취소
}

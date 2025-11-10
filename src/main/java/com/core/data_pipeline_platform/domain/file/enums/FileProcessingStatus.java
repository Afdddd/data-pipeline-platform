package com.core.data_pipeline_platform.domain.file.enums;

public enum FileProcessingStatus {
    PENDING,      // 업로드 완료, 파싱 대기
    PROCESSING,   // 파싱 중
    COMPLETED,    // 성공
    FAILED        // 실패
}

package com.core.data_pipeline_platform.domain.file.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Getter
@AllArgsConstructor
public enum FileType {
    JSON("json", "JSON 파일"),
    EXCEL("xlsx", "Excel 파일"),
    CSV("csv", "CSV 파일"),
    BIN("bin", "바이너리 파일"),
    XML("xml", "XML 파일");

    private final String extension;
    private final String description;

    public static boolean isSupported(String fileName) {
        int idx = fileName.lastIndexOf('.');
        if (idx == -1) return false; // 확장자 없음
        
        String ext = fileName.substring(idx + 1).toLowerCase();
        for (FileType type : values()) {
            if (type.getExtension().equals(ext)) {
                return true;
            }
        }
        return false;
    }

    public static FileType fromFileName(String fileName) {
        int idx = fileName.lastIndexOf('.');
        String ext = fileName.substring(idx + 1).toLowerCase();
        for (FileType type : values()) {
            if (type.getExtension().equals(ext)) {
                return type;
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 형식입니다.");
    }
}

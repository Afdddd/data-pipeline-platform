package com.core.data_pipeline_platform.domain.file.validator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Component
public class FileValidator {

    private final DataSize maxFileSize;

    public FileValidator(@Value("${spring.servlet.multipart.max-file-size}") DataSize maxFileSize){
        this.maxFileSize = maxFileSize;
    }
    
    /**
     * 파일 업로드 기본 검증
     * @param file 업로드된 파일
     * @throws ResponseStatusException 검증 실패 시 BAD_REQUEST
     */
    public void validateFile(MultipartFile file) {
        validateFileExists(file);
        validateFileName(file);
        validateFileSize(file);
        validateFileExtension(file);
    }
    
    /**
     * 파일 존재 여부 검증
     */
    private void validateFileExists(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일이 비어있습니다.");
        }
    }
    
    /**
     * 파일명 검증
     */
    private void validateFileName(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일명이 없습니다.");
        }
    }
    
    /**
     * 파일 크기 검증
     */
    private void validateFileSize(MultipartFile file) {
        if (file.getSize() > maxFileSize.toBytes()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("파일 크기가 제한을 초과합니다. 최대: %s, 현재: %s바이트",
                    maxFileSize.toMegabytes() + "MB",
                    file.getSize())
            );
        }
    }

    /**
     * 파일 확장자 검증
     */
    private void validateFileExtension(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        int idx = fileName.lastIndexOf('.');
        if(idx == -1){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "확장자가 없습니다.");
        }
    }
}

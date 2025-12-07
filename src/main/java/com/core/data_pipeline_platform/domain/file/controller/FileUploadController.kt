package com.core.data_pipeline_platform.domain.file.controller;

import com.core.data_pipeline_platform.domain.file.service.FileUploadService;
import com.core.data_pipeline_platform.domain.file.validator.FileValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("api/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService fileUploadService;
    private final FileValidator fileValidator;

    @PostMapping("/upload")
    public ResponseEntity<Long> uploadFile(@RequestParam("file") MultipartFile file) {
        
        fileValidator.validateFile(file);
        
        Long fileId = fileUploadService.uploadFile(file);
        return ResponseEntity.ok(fileId);
    }
}

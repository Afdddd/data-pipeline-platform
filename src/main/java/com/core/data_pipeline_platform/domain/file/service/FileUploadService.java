package com.core.data_pipeline_platform.domain.file.service;

import com.core.data_pipeline_platform.domain.file.entity.FileEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.core.data_pipeline_platform.domain.file.enums.FileType;
import com.core.data_pipeline_platform.domain.file.repository.FileRepository;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final FileRepository fileRepository;
    private final FileStorageService fileStorageService;

    public Long uploadFile(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        FileType fileType = validateAndGetFileType(fileName);
        validateDuplicateFileName(fileName);
        FileEntity fileEntity = fileStorageService.storeFile(file, fileType);
        FileEntity savedEntity = fileRepository.save(fileEntity);
        return savedEntity.getId();
    }


    private FileType validateAndGetFileType(String fileName) {
        if (!FileType.isSupported(fileName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 형식입니다.");
        }

        return FileType.fromFileName(fileName);
    }

    private void validateDuplicateFileName(String fileName) {
        if (fileRepository.existsByOriginName(fileName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 존재하는 파일 이름입니다.");
        }
    }
}

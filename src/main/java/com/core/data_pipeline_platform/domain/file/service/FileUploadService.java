package com.core.data_pipeline_platform.domain.file.service;

import com.core.data_pipeline_platform.domain.file.entity.FileEntity;
import com.core.data_pipeline_platform.domain.parse.entity.ParsedDataEntity;
import com.core.data_pipeline_platform.domain.parse.repository.ParsedDataRepository;
import com.core.data_pipeline_platform.domain.parse.service.DataParsingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;
import org.springframework.web.multipart.MultipartFile;
import com.core.data_pipeline_platform.domain.file.enums.FileType;
import com.core.data_pipeline_platform.domain.file.repository.FileRepository;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;


@Service
@RequiredArgsConstructor
@Slf4j
public class FileUploadService {

    private final FileRepository fileRepository;
    private final FileStorageService fileStorageService;
    private final DataParsingService dataParsingService;
    private final AsyncFileUploadService asyncFileUploadService;
    private final ParsedDataRepository parsedDataRepository;

    @Transactional
    public Long uploadFile(MultipartFile file) {

        String fileName = file.getOriginalFilename();
        FileType fileType = validateAndGetFileType(fileName);
        validateDuplicateFileName(fileName);

        FileEntity savedFile = saveFile(file, fileType);
        parseAndSaveData(file, fileType, savedFile);

        return savedFile.getId();
    }

    @Transactional
    public Long uploadFile(Path filePath) {
        String fileName = filePath.getFileName().toString();
        FileType fileType = validateAndGetFileType(fileName);
        validateDuplicateFileName(fileName);

        FileEntity savedFile = saveFile(filePath, fileType);
        
        asyncFileUploadService.backgroundParse(filePath, fileType, savedFile.getId());

        return savedFile.getId();
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

    private FileEntity saveFile(MultipartFile file, FileType fileType) {
        try {
            return fileStorageService.storeFile(file, fileType);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 파일 이름입니다.");
        }
    }

    private FileEntity saveFile(Path filePath, FileType fileType) {
        try {
            return fileStorageService.storeFile(filePath, fileType);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 파일 이름입니다.");
        }
    }

    private void parseAndSaveData(MultipartFile file, FileType fileType, FileEntity savedFile) {
        try(InputStream inputStream = file.getInputStream()) {
            ParsedDataEntity parsedDataEntity = dataParsingService
                    .parseToEntity(fileType, inputStream, savedFile);
            parsedDataRepository.save(parsedDataEntity);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "데이터 파싱 실패");
        }
    }
}

package com.core.data_pipeline_platform.domain.file.service;

import com.core.data_pipeline_platform.domain.file.entity.FileEntity;
import com.core.data_pipeline_platform.domain.parse.entity.ParsedDataEntity;
import com.core.data_pipeline_platform.domain.parse.repository.ParsedDataRepository;
import com.core.data_pipeline_platform.domain.parse.service.DataParsingService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.core.data_pipeline_platform.domain.file.enums.FileType;
import com.core.data_pipeline_platform.domain.file.repository.FileRepository;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;


@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final FileRepository fileRepository;
    private final FileStorageService fileStorageService;
    private final DataParsingService dataParsingService;
    private final ParsedDataRepository parsedDataRepository;

    @Transactional
    public Long uploadFile(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        FileType fileType = validateAndGetFileType(fileName);
        validateDuplicateFileName(fileName);

        try {
            FileEntity fileEntity = fileStorageService.storeFile(file, fileType);
            FileEntity savedFile = fileRepository.save(fileEntity);

            ParsedDataEntity parsedDataEntity = dataParsingService
                    .parseToEntity(fileType, file.getInputStream(), savedFile);
            ParsedDataEntity savedEntity = parsedDataRepository.save(parsedDataEntity);

            return savedEntity.getId();
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 파일 이름입니다.");
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "데이터 파싱 실패");
        }
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

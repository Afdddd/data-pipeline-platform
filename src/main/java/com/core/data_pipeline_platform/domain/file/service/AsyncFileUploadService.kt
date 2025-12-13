package com.core.data_pipeline_platform.domain.file.service;

import com.core.data_pipeline_platform.domain.file.entity.FileEntity;
import com.core.data_pipeline_platform.domain.file.enums.FileProcessingStatus;
import com.core.data_pipeline_platform.domain.file.enums.FileType;
import com.core.data_pipeline_platform.domain.parse.entity.ParsedDataEntity;
import com.core.data_pipeline_platform.domain.parse.repository.ParsedDataRepository;
import com.core.data_pipeline_platform.domain.parse.service.DataParsingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncFileUploadService {

    private final ParsedDataRepository parsedDataRepository;
    private final DataParsingService dataParsingService;
    private final com.core.data_pipeline_platform.domain.file.repository.FileRepository fileRepository;

    @Async
    @Transactional
    public void backgroundParse(Path filePath, FileType fileType, Long fileId) {
        try(InputStream inputStream = Files.newInputStream(filePath)) {
            FileEntity fileEntity = fileRepository.findById(fileId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found: " + fileId));
            
            fileEntity.updateStatus(FileProcessingStatus.PROCESSING);

            ParsedDataEntity parsedDataEntity = dataParsingService
                    .parseToEntity(fileType, inputStream, fileEntity);
            parsedDataRepository.save(parsedDataEntity);

            fileEntity.updateStatus(FileProcessingStatus.COMPLETED);

        } catch (IOException e) {
            log.error("파싱 실패: fileId={}", fileId, e);
            FileEntity fileEntity = fileRepository.findById(fileId).orElseThrow();
            fileEntity.updateStatus(FileProcessingStatus.FAILED, e.getMessage());
        }

    }
}

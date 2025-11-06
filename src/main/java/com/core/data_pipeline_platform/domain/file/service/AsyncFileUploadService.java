package com.core.data_pipeline_platform.domain.file.service;

import com.core.data_pipeline_platform.domain.file.entity.FileEntity;
import com.core.data_pipeline_platform.domain.file.enums.FileType;
import com.core.data_pipeline_platform.domain.parse.entity.ParsedDataEntity;
import com.core.data_pipeline_platform.domain.parse.repository.ParsedDataRepository;
import com.core.data_pipeline_platform.domain.parse.service.DataParsingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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

    @Async
    public void backgroundParse(Path filePath, FileType fileType, FileEntity savedFile) {

        log.info("트랜잭션 활성화? {}" , TransactionSynchronizationManager.isActualTransactionActive());

        try(InputStream inputStream = Files.newInputStream(filePath)) {
            ParsedDataEntity parsedDataEntity = dataParsingService
                    .parseToEntity(fileType, inputStream, savedFile);
            parsedDataRepository.save(parsedDataEntity);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "데이터 파싱 실패: " + e.getMessage());
        }

    }
}

package com.core.data_pipeline_platform.domain.file.service;

import com.core.data_pipeline_platform.domain.file.dto.*;
import com.core.data_pipeline_platform.domain.file.entity.ChunkUploadSession;
import com.core.data_pipeline_platform.domain.file.entity.FileEntity;
import com.core.data_pipeline_platform.domain.file.enums.ChunkUploadStatus;
import com.core.data_pipeline_platform.domain.file.enums.FileType;
import com.core.data_pipeline_platform.domain.file.repository.ChunkUploadSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChunkUploadService {

    private final FileStorageService fileStorageService;
    private final ChunkUploadSessionRepository chunkUploadSessionRepository;

    @Transactional
    public ChunkUploadStartResponse startUpload(ChunkUploadStartRequest request) {
        
        FileType fileType = validateAndGetFileType(request.fileName());

        String sessionId = UUID.randomUUID().toString();

        ChunkUploadSession session = ChunkUploadSession.builder()
                .fileType(fileType)
                .fileName(request.fileName())
                .sessionId(sessionId)
                .totalChunks(request.totalChunks())
                .totalSize(request.totalSize())
                .completedChunks(0)
                .status(ChunkUploadStatus.PENDING)
                .chunkInfo("{}")
                .build();

        chunkUploadSessionRepository.save(session);

        return new ChunkUploadStartResponse(sessionId);
    }

    @Transactional
    public ChunkUploadResponse upload(ChunkUploadRequest request) {
        ChunkUploadSession uploadSession = chunkUploadSessionRepository.findBySessionId(request.sessionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "서버에 세션이 없습니다."));

        if (uploadSession.getStatus() == ChunkUploadStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 완료된 세션입니다.");
        }

        if(request.chunkIndex() < 0 || request.chunkIndex() >= uploadSession.getTotalChunks()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 청크 인덱스 입니다.");
        }

        try {
            if(uploadSession.isChunkAlreadyUploaded(request.chunkIndex())) {
                return new ChunkUploadResponse(uploadSession.getProgress());
            }

            fileStorageService.storeChunk(request);
            uploadSession.incrementCompletedChunks();
            uploadSession.updateChunkInfo(request.chunkIndex(), ChunkUploadStatus.COMPLETED);
        } catch (ResponseStatusException e) {
            uploadSession.updateChunkInfo(request.chunkIndex(), ChunkUploadStatus.FAILED);
            throw e; 
        }

        return new ChunkUploadResponse(uploadSession.getProgress());
    }

    @Transactional
    public ChunkUploadCompleteResponse completeUpload(String sessionId) {
        ChunkUploadSession uploadSession = chunkUploadSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "서버에 세션이 없습니다."));

        List<Integer> failedChunks = uploadSession.getFailedChunks();
    
        if (!failedChunks.isEmpty()) {
            return new ChunkUploadCompleteResponse(
                false,
                "일부 청크 업로드 실패",
                failedChunks,
                null
            );
        }

        try {
            FileEntity fileEntity = fileStorageService.mergeChunks(uploadSession);
            uploadSession.updateStatus(ChunkUploadStatus.COMPLETED);

            return new ChunkUploadCompleteResponse(
                true,
                "업로드 완료",
                Collections.emptyList(),
                fileEntity.getId().toString()
            );
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 합치기 실패");
        }
    }


    private FileType validateAndGetFileType(String fileName) {
        if (!FileType.isSupported(fileName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 형식입니다.");
        }

        return FileType.fromFileName(fileName);
    }


}

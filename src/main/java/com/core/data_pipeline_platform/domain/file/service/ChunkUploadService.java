package com.core.data_pipeline_platform.domain.file.service;

import com.core.data_pipeline_platform.domain.file.dto.ChunkUploadRequest;
import com.core.data_pipeline_platform.domain.file.dto.ChunkUploadResponse;
import com.core.data_pipeline_platform.domain.file.dto.ChunkUploadStartRequest;
import com.core.data_pipeline_platform.domain.file.dto.ChunkUploadStartResponse;
import com.core.data_pipeline_platform.domain.file.entity.ChunkUploadSession;
import com.core.data_pipeline_platform.domain.file.enums.ChunkUploadStatus;
import com.core.data_pipeline_platform.domain.file.enums.FileType;
import com.core.data_pipeline_platform.domain.file.repository.ChunkUploadSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

        // db 에서 청크 세션 조회
        ChunkUploadSession uploadSession = chunkUploadSessionRepository.findBySessionId(request.sessionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "서버에 세션이 없습니다."));

        // 임시 파일 생성
        if(fileStorageService.storeChunk(request)){
            uploadSession.incrementCompletedChunks();
            uploadSession.updateChunkInfo(request.chunkIndex(), ChunkUploadStatus.COMPLETED);
        }

        return new ChunkUploadResponse(uploadSession.getProgress());
    }


    private FileType validateAndGetFileType(String fileName) {
        if (!FileType.isSupported(fileName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 형식입니다.");
        }

        return FileType.fromFileName(fileName);
    }
}

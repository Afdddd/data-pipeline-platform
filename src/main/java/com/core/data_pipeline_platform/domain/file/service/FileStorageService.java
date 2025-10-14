package com.core.data_pipeline_platform.domain.file.service;

import com.core.data_pipeline_platform.domain.file.dto.ChunkUploadRequest;
import com.core.data_pipeline_platform.domain.file.entity.ChunkUploadSession;
import com.core.data_pipeline_platform.domain.file.entity.FileEntity;
import com.core.data_pipeline_platform.domain.file.enums.ChunkUploadStatus;
import com.core.data_pipeline_platform.domain.file.enums.FileType;
import com.core.data_pipeline_platform.domain.file.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {
    
    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.chunk-upload-dir}")
    private String chunkUploadDir;

    private final FileRepository fileRepository;

    public FileEntity storeFile(MultipartFile file, FileType fileType) {
        String directoryName = UUID.randomUUID().toString();
        String storedName = UUID.randomUUID().toString();
        String originName = file.getOriginalFilename();
        Path uploadPath = Paths.get(uploadDir, fileType.getExtension(), directoryName);

        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "디렉토리 생성 실패");
        }

        Path targetLocation = uploadPath.resolve(storedName+"."+ fileType.getExtension());

        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장 실패");
        }

        FileEntity fileEntity = FileEntity.builder()
                .storedName(storedName)
                .directoryName(directoryName)
                .fileType(fileType)
                .originName(originName)
                .build();

        return fileRepository.save(fileEntity);
    }

    public void storeChunk(ChunkUploadRequest request) {
        Path dir = Paths.get(chunkUploadDir,request.sessionId());
        try{
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            Path chunkPath = dir.resolve("chunk_" + request.chunkIndex());
            Files.write(chunkPath, request.chunkData(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        }catch (IOException e){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "청크 파일 생성 실패");
        }
    }

    public FileEntity mergeChunks(ChunkUploadSession session) {
        // 임시 파일로 먼저 합치기
        Path tempFile = Paths.get(chunkUploadDir, session.getSessionId(), "temp_merged");
        
        try (FileOutputStream fos = new FileOutputStream(tempFile.toFile())) {
            for (int i = 0; i < session.getTotalChunks(); i++) {
                Path chunkFile = Paths.get(chunkUploadDir, session.getSessionId(), "chunk_" + i);
                if (Files.exists(chunkFile)) {
                    Files.copy(chunkFile, fos);
                } else {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "청크 파일이 존재하지 않습니다: " + i);
                }
            }
        } catch (IOException e) {
            session.updateStatus(ChunkUploadStatus.FAILED);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 합치기 실패");
        }

        FileEntity fileEntity = createFileEntity(session, tempFile);

        // 임시 청크 파일들 삭제
        cleanupTempFiles(session);

        return fileRepository.save(fileEntity);
    }

    private FileEntity createFileEntity(ChunkUploadSession session, Path finalFile) {
        String directoryName = UUID.randomUUID().toString();
        String storedName = UUID.randomUUID().toString();
        String originName = session.getFileName();
        
        // 최종 파일을 새로운 위치로 이동
        Path uploadPath = Paths.get(uploadDir, session.getFileType().getExtension(), directoryName);
        
        try {
            Files.createDirectories(uploadPath);
            Path targetLocation = uploadPath.resolve(storedName + "." + session.getFileType().getExtension());
            Files.move(finalFile, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            return FileEntity.builder()
                    .storedName(storedName)
                    .directoryName(directoryName)
                    .fileType(session.getFileType())
                    .originName(originName)
                    .build();
                    
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 엔티티 생성 실패");
        }
    }

    private void cleanupTempFiles(ChunkUploadSession session) {
        try {
            Path sessionDir = Paths.get(chunkUploadDir, session.getSessionId());
            if (Files.exists(sessionDir)) {
                Files.walk(sessionDir)
                    .sorted(Comparator.reverseOrder()) // 하위 디렉토리부터 삭제
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            // 로그만 남기고 계속 진행
                            System.err.println("임시 파일 삭제 실패: " + path + " - " + e.getMessage());
                        }
                    });
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "임시 파일 정리 실패");
        }
    }

    public void cleanupChunkFiles(ChunkUploadSession session) {
        try {
            Path sessionDir = Paths.get(chunkUploadDir, session.getSessionId());
            if (Files.exists(sessionDir)) {
                Files.walk(sessionDir)
                    .sorted(Comparator.reverseOrder()) // 하위 디렉토리부터 삭제
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            // 로그만 남기고 계속 진행
                            System.err.println("청크 파일 삭제 실패: " + path + " - " + e.getMessage());
                        }
                    });
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "청크 파일 정리 실패");
        }
    }
}

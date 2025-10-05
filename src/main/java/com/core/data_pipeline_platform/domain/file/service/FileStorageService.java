package com.core.data_pipeline_platform.domain.file.service;

import com.core.data_pipeline_platform.domain.file.dto.ChunkUploadRequest;
import com.core.data_pipeline_platform.domain.file.entity.FileEntity;
import com.core.data_pipeline_platform.domain.file.enums.FileType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {
    
    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.chunk-upload-dir}")
    private String chunkUploadDir;

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

        return FileEntity.builder()
                .storedName(storedName)
                .directoryName(directoryName)
                .fileType(fileType)
                .originName(originName)
                .build();
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
}

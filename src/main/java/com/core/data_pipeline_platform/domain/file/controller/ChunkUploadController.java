package com.core.data_pipeline_platform.domain.file.controller;

import com.core.data_pipeline_platform.domain.file.dto.*;
import com.core.data_pipeline_platform.domain.file.service.ChunkUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/files/chunk")
@RequiredArgsConstructor
public class ChunkUploadController {

    private final ChunkUploadService chunkUploadService;

    @PostMapping("/start")
    public ResponseEntity<ChunkUploadStartResponse> startChunkUpload(@RequestBody ChunkUploadStartRequest request) {
        return ResponseEntity.ok(chunkUploadService.startUpload(request));
    }

    @PostMapping("/upload")
    public ResponseEntity<ChunkUploadResponse> uploadChunk(@RequestBody ChunkUploadRequest request) {
        return ResponseEntity.ok(chunkUploadService.upload(request));
    }

    @PostMapping("/complete/{sessionId}")
    public ResponseEntity<ChunkUploadCompleteResponse> completeChunkUpload(@PathVariable String sessionId) {
        return ResponseEntity.ok(chunkUploadService.completeUpload(sessionId));
    }

    @PostMapping("/cancel/{sessionId}")
    public ResponseEntity<ChunkUploadCancelResponse> cancelChunkUpload(@PathVariable String sessionId) {
        return ResponseEntity.ok(chunkUploadService.cancelUpload(sessionId));
    }
}

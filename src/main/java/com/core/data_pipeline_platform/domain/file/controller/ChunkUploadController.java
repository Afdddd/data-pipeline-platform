package com.core.data_pipeline_platform.domain.file.controller;

import com.core.data_pipeline_platform.domain.file.dto.ChunkUploadStartRequest;
import com.core.data_pipeline_platform.domain.file.dto.ChunkUploadStartResponse;
import com.core.data_pipeline_platform.domain.file.service.ChunkUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/files/chunk")
@RequiredArgsConstructor
public class ChunkUploadController {

    private final ChunkUploadService chunkUploadService;

    @PostMapping("/start")
    public ResponseEntity<ChunkUploadStartResponse> startChunkUpload(@RequestBody ChunkUploadStartRequest request){
        return ResponseEntity.ok(chunkUploadService.startUpload(request));
    }
}

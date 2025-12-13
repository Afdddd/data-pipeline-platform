package com.core.data_pipeline_platform.domain.file.controller

import com.core.data_pipeline_platform.domain.file.dto.*
import com.core.data_pipeline_platform.domain.file.service.ChunkUploadService
import jakarta.validation.Valid
import lombok.RequiredArgsConstructor
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/files/chunk")
@RequiredArgsConstructor
class ChunkUploadController {
    private val chunkUploadService: ChunkUploadService? = null

    @PostMapping("/start")
    fun startChunkUpload(@RequestBody request: @Valid ChunkUploadStartRequest): ResponseEntity<ChunkUploadStartResponse?> {
        return ResponseEntity.ok<ChunkUploadStartResponse?>(chunkUploadService!!.startUpload(request))
    }

    @PostMapping("/upload")
    fun uploadChunk(@RequestBody request: @Valid ChunkUploadRequest): ResponseEntity<ChunkUploadResponse?> {
        return ResponseEntity.ok<ChunkUploadResponse?>(chunkUploadService!!.upload(request))
    }

    @PostMapping("/complete/{sessionId}")
    fun completeChunkUpload(@PathVariable sessionId: String?): ResponseEntity<ChunkUploadCompleteResponse?> {
        return ResponseEntity.ok<ChunkUploadCompleteResponse?>(chunkUploadService!!.completeUpload(sessionId))
    }

    @PostMapping("/cancel/{sessionId}")
    fun cancelChunkUpload(@PathVariable sessionId: String?): ResponseEntity<ChunkUploadCancelResponse?> {
        return ResponseEntity.ok<ChunkUploadCancelResponse?>(chunkUploadService!!.cancelUpload(sessionId))
    }
}

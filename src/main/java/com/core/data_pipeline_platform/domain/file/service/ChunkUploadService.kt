package com.core.data_pipeline_platform.domain.file.service

import com.core.data_pipeline_platform.common.annotation.Retryable
import com.core.data_pipeline_platform.domain.file.dto.*
import com.core.data_pipeline_platform.domain.file.entity.ChunkUploadSession
import com.core.data_pipeline_platform.domain.file.enums.ChunkUploadStatus
import com.core.data_pipeline_platform.domain.file.enums.FileType
import com.core.data_pipeline_platform.domain.file.enums.FileType.Companion.fromFileName
import com.core.data_pipeline_platform.domain.file.enums.FileType.Companion.isSupported
import com.core.data_pipeline_platform.domain.file.repository.ChunkUploadSessionRepository
import lombok.RequiredArgsConstructor
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.*
import java.util.function.Supplier

@Service
@RequiredArgsConstructor
class ChunkUploadService(
    private val fileStorageService: FileStorageService,
    private val chunkUploadSessionRepository: ChunkUploadSessionRepository
){

    @Transactional
    fun startUpload(request: ChunkUploadStartRequest): ChunkUploadStartResponse {
        val fileType = validateAndGetFileType(request.fileName!!)

        val sessionId: String = UUID.randomUUID().toString()

        val session = ChunkUploadSession(
            fileType = fileType,
            fileName = request.fileName,
            sessionId = sessionId,
            totalChunks = request.totalChunks,
            completedChunks = 0,
            status = ChunkUploadStatus.PENDING,
            chunkInfo = "{}"
        )

        chunkUploadSessionRepository.save<ChunkUploadSession?>(session)

        return ChunkUploadStartResponse(sessionId)
    }

    @Transactional
    @Retryable
    fun upload(request: ChunkUploadRequest): ChunkUploadResponse {
        val uploadSession = chunkUploadSessionRepository.findBySessionId(request.sessionId!!)
            .orElseThrow(Supplier {
                ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "서버에 세션이 없습니다."
                )
            })

        if (uploadSession == null) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "저장된 session이 없습니다.")
        }

        if (uploadSession.status === ChunkUploadStatus.COMPLETED) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 완료된 세션입니다.")
        }

        if (uploadSession.status === ChunkUploadStatus.CANCELLED) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "취소된 세션입니다.")
        }

        if (request.chunkIndex < 0 || request.chunkIndex >= uploadSession.totalChunks!!) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 청크 인덱스 입니다.")
        }

        try {
            if (uploadSession.isChunkAlreadyUploaded(request.chunkIndex)) {
                return ChunkUploadResponse(uploadSession.progress)
            }

            fileStorageService.storeChunk(request)
            uploadSession.incrementCompletedChunks()
            uploadSession.updateChunkInfo(request.chunkIndex, ChunkUploadStatus.COMPLETED)

            if (uploadSession.status !== ChunkUploadStatus.IN_PROGRESS) {
                uploadSession.updateStatus(ChunkUploadStatus.IN_PROGRESS)
            }
        } catch (e: ResponseStatusException) {
            uploadSession.updateChunkInfo(request.chunkIndex, ChunkUploadStatus.FAILED)
            throw e
        }

        return ChunkUploadResponse(uploadSession.progress)
    }

    @Transactional
    fun completeUpload(sessionId: String?): ChunkUploadCompleteResponse {
        val uploadSession = chunkUploadSessionRepository.findBySessionId(sessionId!!)
            .orElseThrow(Supplier {
                ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "서버에 세션이 없습니다."
                )
            })

        val failedChunks = uploadSession!!.failedChunks

        if (!failedChunks.isEmpty()) {
            uploadSession.updateStatus(ChunkUploadStatus.FAILED)
            return ChunkUploadCompleteResponse(
                false,
                "일부 청크 업로드 실패",
                failedChunks,
                null
            )
        }

        try {
            val fileEntity = fileStorageService.mergeChunks(uploadSession)
            uploadSession.updateStatus(ChunkUploadStatus.COMPLETED)

            return ChunkUploadCompleteResponse(
                true,
                "업로드 완료",
                mutableListOf(),
                fileEntity.id.toString()
            )
        } catch (e: Exception) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 합치기 실패 ${e.message}")
        }
    }

    @Transactional
    fun cancelUpload(sessionId: String?): ChunkUploadCancelResponse {
        val uploadSession = chunkUploadSessionRepository.findBySessionId(sessionId!!)
            .orElseThrow(Supplier {
                ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "서버에 세션이 없습니다."
                )
            })

        if (uploadSession == null) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "저장된 세션이 없습니다.")
        }

        // 이미 완료되거나 취소된 세션은 처리하지 않음
        if (uploadSession.status === ChunkUploadStatus.COMPLETED) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 완료된 세션입니다.")
        }

        if (uploadSession.status === ChunkUploadStatus.CANCELLED) {
            return ChunkUploadCancelResponse(true, "이미 취소된 세션입니다.")
        }

        // 상태를 CANCELLED로 변경
        uploadSession.updateStatus(ChunkUploadStatus.CANCELLED)

        // 임시 청크 파일들 정리
        try {
            fileStorageService.cleanupChunkFiles(uploadSession)
        } catch (e: Exception) {
            // 파일 정리 실패해도 취소는 성공으로 처리
            System.err.println("청크 파일 정리 실패: " + e.message)
        }

        return ChunkUploadCancelResponse(true, "업로드가 취소되었습니다.")
    }

    private fun validateAndGetFileType(fileName: String): FileType {
        if (!isSupported(fileName)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 형식입니다.")
        }

        return fromFileName(fileName)
    }
}

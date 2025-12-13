package com.core.data_pipeline_platform.domain.file.repository

import com.core.data_pipeline_platform.domain.file.entity.ChunkUploadSession
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ChunkUploadSessionRepository : JpaRepository<ChunkUploadSession, Long> {
    fun findBySessionId(sessionId: String): Optional<ChunkUploadSession>
}

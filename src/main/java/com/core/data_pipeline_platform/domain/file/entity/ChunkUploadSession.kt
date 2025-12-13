package com.core.data_pipeline_platform.domain.file.entity

import com.core.data_pipeline_platform.domain.file.enums.ChunkUploadStatus
import com.core.data_pipeline_platform.domain.file.enums.FileType
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.util.Map

/**
 * 청크 업로드 세션을 관리하는 엔티티
 */
@Entity
@Table(name = "chunk_upload_session")
class ChunkUploadSession(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @OneToOne
    @JoinColumn(name = "file_id")
    var file: FileEntity? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false)
    var fileType: FileType? = null,

    @Column(name = "file_name", nullable = false)
    var fileName: String? = null,

    @Column(name = "session_id", unique = true, nullable = false)
    var sessionId: String? = null,

    @Column(name = "total_size", nullable = false)
    var totalSize: Long? = null,

    @Column(name = "total_chunks", nullable = false)
    var totalChunks: Int? = null,

    @Column(name = "completed_chunks", nullable = false)
    var completedChunks: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: ChunkUploadStatus? = null,

    @Column(name = "chunk_info", columnDefinition = "JSON")
    var chunkInfo: String? = null,
) {
    @CreationTimestamp
    @Column(name = "created_at")
    private var createdAt: LocalDateTime? = null

    @UpdateTimestamp
    @Column(name = "updated_at")
    private var updatedAt: LocalDateTime? = null

    @Version
    private var version: Long? = null

    fun incrementCompletedChunks() {
        this.completedChunks = this.completedChunks!! + 1
    }

    fun updateChunkInfo(chunkIndex: Int, status: ChunkUploadStatus) {
        var chunkInfoMap: MutableMap<String?, String?> = HashMap<String?, String?>()

        try {
            if (chunkInfo != null && !chunkInfo!!.isEmpty()) {
                chunkInfoMap = objectMapper.readValue<MutableMap<String?, String?>>(
                    chunkInfo,
                    object : TypeReference<MutableMap<String?, String?>?>() {})
            }

            chunkInfoMap[chunkIndex.toString()] = status.name

            this.chunkInfo = objectMapper.writeValueAsString(chunkInfoMap)
        } catch (e: Exception) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "chunkInfo update 실패.")
        }
    }

    fun isChunkAlreadyUploaded(chunkIndex: Int): Boolean {
        try {
            if (chunkInfo == null || chunkInfo!!.isEmpty()) {
                return false
            }

            val chunkMap: MutableMap<String?, String?> = objectMapper.readValue<MutableMap<String?, String?>>(
                chunkInfo,
                object : TypeReference<MutableMap<String?, String?>?>() {})
            val status = chunkMap.get(chunkIndex.toString())
            return "COMPLETED" == status
        } catch (e: Exception) {
            return false
        }
    }

    val failedChunks: MutableList<Int>
        get() {
            try {
                if (chunkInfo == null || chunkInfo!!.isEmpty()) {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "chunkInfo가 없습니다.")
                }

                val chunkMap: MutableMap<String, String> =
                    objectMapper.readValue(
                        chunkInfo,
                        object :
                            TypeReference<MutableMap<String, String>>() {})
                return chunkMap.entries.stream()
                    .filter { entry: MutableMap.MutableEntry<String, String> -> "COMPLETED" != entry.value }
                    .map { entry -> entry.key }
                    .map { s: String -> s.toInt() }
                    .toList()
            } catch (e: Exception) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "getFailedChunks 실패 : ${e.message}")
            }
        }


    fun updateStatus(status: ChunkUploadStatus?) {
        this.status = status
    }

    val progress: Int
        get() = ((completedChunks!!.toDouble() / totalChunks!!) * 100).toInt()

    companion object {
        private val objectMapper = ObjectMapper()
    }
}

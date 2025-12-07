package com.core.data_pipeline_platform.domain.file.service

import com.core.data_pipeline_platform.domain.file.dto.ChunkUploadRequest
import com.core.data_pipeline_platform.domain.file.entity.ChunkUploadSession
import com.core.data_pipeline_platform.domain.file.entity.FileEntity
import com.core.data_pipeline_platform.domain.file.enums.ChunkUploadStatus
import com.core.data_pipeline_platform.domain.file.enums.FileType
import com.core.data_pipeline_platform.domain.file.repository.FileRepository
import lombok.RequiredArgsConstructor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.*
import java.util.*

@Service
@RequiredArgsConstructor
class FileStorageService @Autowired constructor(
    private val fileRepository: FileRepository
) {
    @Value("\${file.upload-dir}")
    private val uploadDir: String? = null

    @Value("\${file.chunk-upload-dir}")
    private val chunkUploadDir: String? = null


    fun storeFile(file: MultipartFile, fileType: FileType): FileEntity {
        val directoryName: String = UUID.randomUUID().toString()
        val storedName: String = UUID.randomUUID().toString()
        val originName = file.originalFilename
        val uploadPath = Paths.get(uploadDir!!, fileType.extension, directoryName)

        try {
            Files.createDirectories(uploadPath)
        } catch (e: IOException) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "디렉토리 생성 실패 : ${e.message}")
        }

        val targetLocation = uploadPath.resolve(storedName + "." + fileType.extension)

        try {
            Files.copy(file.inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: IOException) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장 실패 : ${e.message}")
        }


        val fileEntity = FileEntity(
            storedName = storedName,
            directoryName = directoryName,
            fileType = fileType,
            originName = originName
        )
        return fileRepository.save<FileEntity>(fileEntity)
    }

    fun storeFile(filePath: Path, fileType: FileType): FileEntity {
        val directoryName: String = UUID.randomUUID().toString()
        val storedName: String = UUID.randomUUID().toString()
        val originName = filePath.fileName.toString()
        val uploadPath = Paths.get(uploadDir!!, fileType.extension, directoryName)

        try {
            Files.createDirectories(uploadPath)
        } catch (e: IOException) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "디렉토리 생성 실패 : ${e.message}")
        }

        val targetLocation = uploadPath.resolve(storedName + "." + fileType.extension)

        try {
            Files.copy(filePath, targetLocation, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: IOException) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장 실패 : ${e.message}")
        }

        val fileEntity = FileEntity(
            storedName = storedName,
            directoryName = directoryName,
            fileType = fileType,
            originName = originName
        )

        return fileRepository.save<FileEntity>(fileEntity)
    }

    fun storeChunk(request: ChunkUploadRequest) {
        val dir = Paths.get(chunkUploadDir!!, request.sessionId)
        try {
            if (!Files.exists(dir)) {
                Files.createDirectories(dir)
            }

            val chunkPath = dir.resolve("chunk_" + request.chunkIndex)
            Files.write(chunkPath, request.chunkData!!, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
        } catch (e: IOException) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "청크 파일 생성 실패 : ${e.message}")
        }
    }

    fun mergeChunks(session: ChunkUploadSession): FileEntity {
        // 임시 파일로 먼저 합치기
        val tempFile = Paths.get(chunkUploadDir!!, session.sessionId, "temp_merged")

        try {
            FileOutputStream(tempFile.toFile()).use { fos ->
                for (i in 0..<session.totalChunks) {
                    val chunkFile = Paths.get(chunkUploadDir, session.sessionId, "chunk_$i")
                    if (Files.exists(chunkFile)) {
                        Files.copy(chunkFile, fos)
                    } else {
                        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "청크 파일이 존재하지 않습니다: $i")
                    }
                }
            }
        } catch (e: IOException) {
            session.updateStatus(ChunkUploadStatus.FAILED)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 합치기 실패 : ${e.message}")
        }

        val fileEntity = createFileEntity(session, tempFile)

        // 임시 청크 파일들 삭제
        cleanupTempFiles(session)

        return fileRepository.save<FileEntity>(fileEntity)
    }

    private fun createFileEntity(session: ChunkUploadSession, finalFile: Path): FileEntity {
        val directoryName: String = UUID.randomUUID().toString()
        val storedName: String = UUID.randomUUID().toString()
        val originName = session.fileName


        // 최종 파일을 새로운 위치로 이동
        val uploadPath = Paths.get(uploadDir!!, session.fileType.extension, directoryName)

        try {
            Files.createDirectories(uploadPath)
            val targetLocation = uploadPath.resolve(storedName + "." + session.fileType.extension)
            Files.move(finalFile, targetLocation, StandardCopyOption.REPLACE_EXISTING)

            return FileEntity(
                storedName = storedName,
                directoryName = directoryName,
                fileType = session.fileType,
                originName = originName
            )
        } catch (e: IOException) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 엔티티 생성 실패 : ${e.message}")
        }
    }

    private fun cleanupTempFiles(session: ChunkUploadSession) {
        try {
            val sessionDir = Paths.get(chunkUploadDir!!, session.sessionId)
            if (Files.exists(sessionDir)) {
                Files.walk(sessionDir)
                    .sorted(Comparator.reverseOrder<Path>()) // 하위 디렉토리부터 삭제
                    .forEach { path: Path? ->
                        try {
                            Files.deleteIfExists(path!!)
                        } catch (e: IOException) {
                            // 로그만 남기고 계속 진행
                            System.err.println("임시 파일 삭제 실패: " + path + " - " + e.message)
                        }
                    }
            }
        } catch (e: IOException) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "임시 파일 정리 실패 : ${e.message}")
        }
    }

    fun cleanupChunkFiles(session: ChunkUploadSession) {
        try {
            val sessionDir = Paths.get(chunkUploadDir!!, session.sessionId)
            if (Files.exists(sessionDir)) {
                Files.walk(sessionDir)
                    .sorted(Comparator.reverseOrder<Path>()) // 하위 디렉토리부터 삭제
                    .forEach { path: Path? ->
                        try {
                            Files.deleteIfExists(path!!)
                        } catch (e: IOException) {
                            // 로그만 남기고 계속 진행
                            System.err.println("청크 파일 삭제 실패: " + path + " - " + e.message)
                        }
                    }
            }
        } catch (e: IOException) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "청크 파일 정리 실패 : ${e.message}")
        }
    }
}

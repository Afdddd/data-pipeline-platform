package com.core.data_pipeline_platform.domain.file.service

import com.core.data_pipeline_platform.domain.file.entity.FileEntity
import com.core.data_pipeline_platform.domain.file.enums.FileType
import com.core.data_pipeline_platform.domain.file.enums.FileType.Companion.fromFileName
import com.core.data_pipeline_platform.domain.file.enums.FileType.Companion.isSupported
import com.core.data_pipeline_platform.domain.file.repository.FileRepository
import com.core.data_pipeline_platform.domain.parse.entity.ParsedDataEntity
import com.core.data_pipeline_platform.domain.parse.repository.ParsedDataRepository
import com.core.data_pipeline_platform.domain.parse.service.DataParsingService
import lombok.extern.slf4j.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.io.IOException
import java.nio.file.Path

@Service
@Slf4j
class FileUploadService @Autowired constructor(
    private val fileRepository: FileRepository,
    private val fileStorageService: FileStorageService,
    private val dataParsingService: DataParsingService,
    private val asyncFileUploadService: AsyncFileUploadService,
    private val parsedDataRepository: ParsedDataRepository
){

    @Transactional
    fun uploadFile(file: MultipartFile): Long? {
        val fileName = file.originalFilename
        val fileType = validateAndGetFileType(fileName!!)
        validateDuplicateFileName(fileName)

        val savedFile = saveFile(file, fileType)
        parseAndSaveData(file, fileType, savedFile)

        return savedFile.id
    }

    @Transactional
    fun uploadFile(filePath: Path): Long? {
        val fileName = filePath.fileName.toString()
        val fileType = validateAndGetFileType(fileName)
        validateDuplicateFileName(fileName)

        val savedFile = saveFile(filePath, fileType)

        asyncFileUploadService.backgroundParse(filePath, fileType, savedFile.id)

        return savedFile.id
    }


    private fun validateAndGetFileType(fileName: String): FileType {
        if (!isSupported(fileName)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 형식입니다.")
        }

        return fromFileName(fileName)
    }

    private fun validateDuplicateFileName(fileName: String?) {
        if (fileRepository.existsByOriginName(fileName)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 존재하는 파일 이름입니다.")
        }
    }

    private fun saveFile(file: MultipartFile, fileType: FileType): FileEntity {
        try {
            return fileStorageService.storeFile(file, fileType)
        } catch (e: DataIntegrityViolationException) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 파일 이름입니다. : ${e.message}")
        }
    }

    private fun saveFile(filePath: Path, fileType: FileType): FileEntity {
        try {
            return fileStorageService.storeFile(filePath, fileType)
        } catch (e: DataIntegrityViolationException) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 파일 이름입니다. : ${e.message}")
        }
    }

    private fun parseAndSaveData(file: MultipartFile, fileType: FileType?, savedFile: FileEntity?) {
        try {
            file.getInputStream().use { inputStream ->
                val parsedDataEntity = dataParsingService
                    .parseToEntity(fileType, inputStream, savedFile)
                parsedDataRepository.save(parsedDataEntity)
            }
        } catch (e: IOException) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "데이터 파싱 실패 : ${e.message}")
        }
    }
}

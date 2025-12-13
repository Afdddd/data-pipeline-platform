package com.core.data_pipeline_platform.domain.file.service

import com.core.data_pipeline_platform.domain.file.entity.FileEntity
import com.core.data_pipeline_platform.domain.file.enums.FileProcessingStatus
import com.core.data_pipeline_platform.domain.file.enums.FileType
import com.core.data_pipeline_platform.domain.file.repository.FileRepository
import com.core.data_pipeline_platform.domain.parse.entity.ParsedDataEntity
import com.core.data_pipeline_platform.domain.parse.repository.ParsedDataRepository
import com.core.data_pipeline_platform.domain.parse.service.DataParsingService
import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import org.springframework.http.HttpStatus
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Supplier

@Slf4j
@Service
@RequiredArgsConstructor
class AsyncFileUploadService {
    private val parsedDataRepository: ParsedDataRepository? = null
    private val dataParsingService: DataParsingService? = null
    private val fileRepository: FileRepository? = null

    @Async
    @Transactional
    fun backgroundParse(filePath: Path, fileType: FileType, fileId: Long) {
        try {
            Files.newInputStream(filePath).use { inputStream ->
                val fileEntity = fileRepository!!.findById(fileId)
                    .orElseThrow(Supplier {
                        ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "File not found: $fileId"
                        )
                    })
                fileEntity!!.updateStatus(FileProcessingStatus.PROCESSING)

                val parsedDataEntity = dataParsingService!!
                    .parseToEntity(fileType, inputStream, fileEntity)
                parsedDataRepository!!.save<ParsedDataEntity?>(parsedDataEntity)
                fileEntity.updateStatus(FileProcessingStatus.COMPLETED)
            }
        } catch (e: IOException) {
            val fileEntity: FileEntity = fileRepository!!.findById(fileId).orElseThrow()!!
            fileEntity.updateStatus(FileProcessingStatus.FAILED, e.message)
        }
    }
}

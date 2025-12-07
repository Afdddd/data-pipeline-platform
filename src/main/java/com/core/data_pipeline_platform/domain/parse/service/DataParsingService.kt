package com.core.data_pipeline_platform.domain.parse.service

import com.core.data_pipeline_platform.domain.file.entity.FileEntity
import com.core.data_pipeline_platform.domain.file.enums.FileType
import com.core.data_pipeline_platform.domain.parse.entity.ParsedDataEntity
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import lombok.RequiredArgsConstructor
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.io.InputStream

@Service
@RequiredArgsConstructor
class DataParsingService {
    private val parserFactory: ParserFactory? = null
    private val objectMapper: ObjectMapper? = null

    fun parseToEntity(fileType: FileType, inputStream: InputStream, file: FileEntity): ParsedDataEntity {
        try {
            val parser = parserFactory!!.getParser(fileType)
            val maps = parser.parseData(fileType, inputStream)

            val jsonData = objectMapper!!.writeValueAsString(maps)

            return ParsedDataEntity.builder()
                .file(file)
                .data(jsonData)
                .build()
        } catch (e: JsonProcessingException) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파싱 실패 : ${e.message}")
        }
    }
}

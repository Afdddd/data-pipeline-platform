package com.core.data_pipeline_platform.domain.parse.service

import com.core.data_pipeline_platform.domain.file.enums.FileType
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import lombok.RequiredArgsConstructor
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.io.IOException
import java.io.InputStream

@Service
@RequiredArgsConstructor
class JsonDataParser : DataParser {
    private val objectMapper: ObjectMapper? = null

    override fun parseData(fileType: FileType, inputStream: InputStream): MutableList<MutableMap<String, Any>> {
        if (fileType != FileType.JSON) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "파일 타입 불일치: JSON이어야 합니다.")
        }

        try {
            return objectMapper!!.readValue(
                inputStream,
                object : TypeReference<MutableList<MutableMap<String, Any>>>() {
                }
            )
        } catch (e: IOException) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Json 파싱 실패 : ${e.message}")
        }
    }

    override val supportedFileType: FileType
        get() = FileType.JSON
}

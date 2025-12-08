package com.core.data_pipeline_platform.domain.generator.controller

import com.core.data_pipeline_platform.domain.file.enums.FileType
import com.core.data_pipeline_platform.domain.generator.dto.GenerateRequest
import com.core.data_pipeline_platform.domain.generator.service.*
import jakarta.validation.Valid
import lombok.RequiredArgsConstructor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.*

@RestController
@RequestMapping("api/generate")
@RequiredArgsConstructor
class FileGenerateController @Autowired constructor(
    private val jsonFileGenerator: JsonFileGenerator,
    private val csvFileGenerator: CsvFileGenerator,
    private val xmlFileGenerator: XmlFileGenerator,
    private val binFileGenerator: BinFileGenerator
) {

    @PostMapping(value = ["/{format}"])
    fun generateFile(
        @PathVariable format: String,
        @RequestBody request: @Valid GenerateRequest
    ): ResponseEntity<ByteArray?> {
        val fileGenerator = getGenerator(format)
        val content = fileGenerator.generateFile(request)
        val mimeType = getMimeType(format)

        return ResponseEntity.ok()
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + request.fileName + "\""
            )
            .header(HttpHeaders.CONTENT_TYPE, mimeType)
            .header(HttpHeaders.CONTENT_LENGTH, content.size.toString())
            .body<ByteArray?>(content)
    }

    private fun getGenerator(format: String): FileGenerator {
        val f = format.lowercase(Locale.getDefault())
        return when (f) {
            "json" -> jsonFileGenerator
            "csv" -> csvFileGenerator
            "xml" -> xmlFileGenerator
            "bin" -> binFileGenerator
            else -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 형식입니다. [$format]")
        }
    }

    private fun getMimeType(format: String?): String {
        return FileType.getMimeType(format)
    }
}

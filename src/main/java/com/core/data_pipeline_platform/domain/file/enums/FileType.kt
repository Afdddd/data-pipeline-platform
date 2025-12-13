package com.core.data_pipeline_platform.domain.file.enums

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

enum class FileType(
    val extension: String,
    val description: String,
    val mimeType: String
) {
    JSON("json", "JSON 파일", "application/json"),
    CSV("csv", "CSV 파일", "text/csv"),
    BIN("bin", "바이너리 파일", "application/octet-stream"),
    XML("xml", "XML 파일", "application/xml");

    companion object {
        @JvmStatic
        fun isSupported(fileName: String): Boolean {
            val idx = fileName.lastIndexOf('.')
            if (idx == -1) return false // 확장자 없음

            val ext = fileName.substring(idx + 1).lowercase()
            return entries.any {it.extension == ext}
        }

        @JvmStatic
        fun fromFileName(fileName: String): FileType {
            val idx = fileName.lastIndexOf('.')
            val ext = fileName.substring(idx + 1).lowercase()
            return entries.find { it.extension == ext}
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 형식입니다.")
        }

        @JvmStatic
        fun getMimeType(extension: String?): String {
            return entries.find { it.extension == extension }?.mimeType
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 형식입니다.")
        }

        @JvmStatic
        fun fromExtension(extension: String?): FileType {
            return entries.find { it.extension == extension }
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 형식입니다.")
        }

        @JvmStatic
        fun getFileDescription(extension: String?): String {
            return entries.find { it.extension == extension }?.description
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 형식입니다.")
        }
    }
}
